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

trait Producer[A, F[_]] {

  def produce: F[A] => F[Unit]
}

object Producer {

  final private class ProducerContainer[F[_]: FlatMap, A](ffa: F[Producer[A, F]]) extends Producer[A, F] {
    def produce: F[A] => F[Unit] = fa => ffa.flatMap(_.produce(fa))
  }

  implicit def embed[A]: Embed[Producer[A, *[_]]] =
    new Embed[Producer[A, *[_]]] {

      def embed[F[_]: FlatMap](ft: F[Producer[A, F]]): Producer[A, F] =
        new ProducerContainer(ft)
    }

  implicit def invK[A]: InvariantK[Producer[A, *[_]]] =
    new InvariantK[Producer[A, *[_]]] {

      def imapK[F[_], G[_]](af: Producer[A, F])(fk: F ~> G)(gK: G ~> F): Producer[A, G] =
        new Producer[A, G] {
          def produce: G[A] => G[Unit] = ga => fk(af.produce(gK(ga)))
        }
    }

  def make[
    F[_]: FlatMap: WithContext[*[_], ProducerConfig],
    G[_],
    A: KeyEncoder
  ](implicit
    isoK: IsoK[F, Stream[G, *]],
    makeProducer: MakeKafkaProducer[G, String, A]
  ): Producer[A, F] =
    (context[F] map (conf => new Live(conf).imapK(isoK.fromF)(isoK.tof))).embed

  final private class Live[F[_], A: KeyEncoder](
    config: ProducerConfig
  )(implicit makeProducer: MakeKafkaProducer[F, String, A])
    extends Producer[A, Stream[F, *]] {

    def produce: Pipe[F, A, Unit] =
      _.map { v =>
        ProducerRecords.one(ProducerRecord(config.topicId.value, KeyEncoder[A].encode(v), v))
      }.through(makeProducer(config)).drain
  }
}
