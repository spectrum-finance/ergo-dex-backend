package org.ergoplatform.dex.streaming

import cats.tagless.InvariantK
import cats.{~>, FlatMap}
import cats.tagless.syntax.invariantK._
import fs2._
import fs2.kafka.{ProducerRecord, ProducerRecords}
import org.ergoplatform.dex.configs.ProducerConfig
import tofu.WithContext
import tofu.higherKind.Embed
import tofu.lift.IsoK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._

trait Producer[K, V, F[_]] {

  def produce: F[Record[K, V]] => F[Unit]
}

object Producer {

  final private class ProducerContainer[F[_]: FlatMap, K, V](ffa: F[Producer[K, V, F]]) extends Producer[K, V, F] {
    def produce: F[Record[K, V]] => F[Unit] = fa => ffa.flatMap(_.produce(fa))
  }

  implicit def embed[K, V]: Embed[Producer[K, V, *[_]]] =
    new Embed[Producer[K, V, *[_]]] {

      def embed[F[_]: FlatMap](ft: F[Producer[K, V, F]]): Producer[K, V, F] =
        new ProducerContainer(ft)
    }

  implicit def invK[K, V]: InvariantK[Producer[K, V, *[_]]] =
    new InvariantK[Producer[K, V, *[_]]] {

      def imapK[F[_], G[_]](af: Producer[K, V, F])(fk: F ~> G)(gK: G ~> F): Producer[K, V, G] =
        new Producer[K, V, G] {
          def produce: G[Record[K, V]] => G[Unit] = ga => fk(af.produce(gK(ga)))
        }
    }

  def make[
    F[_]: FlatMap: WithContext[*[_], ProducerConfig],
    G[_],
    K,
    V
  ](implicit
    isoK: IsoK[F, Stream[G, *]],
    makeProducer: MakeKafkaProducer[G, K, V]
  ): Producer[K, V, F] =
    (context[F] map (conf => new Live(conf).imapK(isoK.fromF)(isoK.tof))).embed

  final private class Live[F[_], K, V](
    config: ProducerConfig
  )(implicit makeProducer: MakeKafkaProducer[F, K, V])
    extends Producer[K, V, Stream[F, *]] {

    def produce: Pipe[F, Record[K, V], Unit] =
      _.map { case Record(k, v) =>
        ProducerRecords.one(ProducerRecord(config.topicId.value, k, v))
      }.through(makeProducer(config)).drain
  }
}
