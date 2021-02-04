package org.ergoplatform.dex.executor.modules

import cats.Functor
import org.ergoplatform.ErgoBoxCandidate
import org.ergoplatform.dex.executor.context.BlockchainContext
import org.ergoplatform.dex.executor.domain.errors.ExhaustedOutputValue
import scorex.crypto.encode.Base16
import tofu.syntax.context._
import tofu.syntax.monadic._

trait OutputValueValidation[F[_]] {

  def validate(out: ErgoBoxCandidate): F[Either[ExhaustedOutputValue, Unit]]
}

object OutputValueValidation {

  implicit def instance[F[_]: Functor: BlockchainContext.Has]: OutputValueValidation[F] =
    (out: ErgoBoxCandidate) =>
      context.map { ctx =>
        val minValue = (out.bytesWithNoRef.length + 33) * ctx.nanoErgsPerByte
        if (out.value >= minValue) Right(())
        else Left(ExhaustedOutputValue(out.value, minValue, ctx.nanoErgsPerByte))
      }
}
