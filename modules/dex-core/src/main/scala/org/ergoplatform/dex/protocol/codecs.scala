package org.ergoplatform.dex.protocol

import java.math.BigInteger

import cats.syntax.either._
import io.circe._
import io.circe.syntax._
import org.ergoplatform.ErgoBox.{BoxId, NonMandatoryRegisterId, TokenId}
import org.ergoplatform._
import org.ergoplatform.settings.ErgoAlgos
import scorex.crypto.authds.{ADDigest, ADKey}
import scorex.crypto.hash.Digest32
import scorex.util.ModifierId
import sigmastate.Values.{ErgoTree, EvaluatedValue}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.Extensions._
import sigmastate.eval._
import sigmastate.interpreter.{ContextExtension, ProverResult}
import sigmastate.serialization.{GroupElementSerializer, ValueSerializer, ErgoTreeSerializer => SigmaErgoTreeSerializer}
import sigmastate.{AvlTreeData, AvlTreeFlags, SType}
import special.collection.Coll

import scala.util.Try

object codecs {

  implicit def eltCodec: io.circe.Codec[ErgoLikeTransaction] =
    io.circe.Codec.from(ergoLikeTransactionDecoder, ergoLikeTransactionEncoder)

  def fromTry[T](tryResult: Try[T])(implicit cursor: ACursor): Either[DecodingFailure, T] =
    tryResult.fold(e => Left(DecodingFailure(e.toString, cursor.history)), Right.apply)

  def fromOption[T](maybeResult: Option[T])(implicit cursor: ACursor): Either[DecodingFailure, T] =
    maybeResult.fold[Either[DecodingFailure, T]](
      Left(DecodingFailure("No value found", cursor.history))
    )(Right.apply)

  def fromThrows[T](throwsBlock: => T)(implicit cursor: ACursor): Either[DecodingFailure, T] =
    Either.catchNonFatal(throwsBlock).leftMap(e => DecodingFailure(e.toString, cursor.history))

  private def bytesDecoder[T](transform: Array[Byte] => T): Decoder[T] =
    Decoder.instance { implicit cursor =>
      for {
        str   <- cursor.as[String]
        bytes <- fromTry(ErgoAlgos.decode(str))
      } yield transform(bytes)
    }

  implicit val sigmaBigIntEncoder: Encoder[special.sigma.BigInt] =
    Encoder.instance { bigInt =>
      JsonNumber
        .fromDecimalStringUnsafe(bigInt.asInstanceOf[WrapperOf[BigInteger]].wrappedValue.toString)
        .asJson
    }

  implicit val sigmaBigIntDecoder: Decoder[special.sigma.BigInt] =
    Decoder.instance { implicit cursor =>
      for {
        jsonNumber <- cursor.as[JsonNumber]
        bigInt     <- fromOption(jsonNumber.toBigInt)
      } yield CBigInt(bigInt.bigInteger)
    }

  implicit val arrayBytesEncoder: Encoder[Array[Byte]] =
    Encoder.instance(ErgoAlgos.encode(_).asJson)
  implicit val arrayBytesDecoder: Decoder[Array[Byte]] = bytesDecoder(x => x)

  implicit val collBytesEncoder: Encoder[Coll[Byte]] = Encoder.instance(ErgoAlgos.encode(_).asJson)
  implicit val collBytesDecoder: Decoder[Coll[Byte]] = bytesDecoder(Colls.fromArray(_))

  implicit val adKeyEncoder: Encoder[ADKey] = Encoder.instance(_.array.asJson)
  implicit val adKeyDecoder: Decoder[ADKey] = bytesDecoder(ADKey @@ _)

  implicit val adDigestEncoder: Encoder[ADDigest] = Encoder.instance(_.array.asJson)
  implicit val adDigestDecoder: Decoder[ADDigest] = bytesDecoder(ADDigest @@ _)

  implicit val digest32Encoder: Encoder[Digest32] = Encoder.instance(_.array.asJson)
  implicit val digest32Decoder: Decoder[Digest32] = bytesDecoder(Digest32 @@ _)

  implicit val proveDlogEncoder: Encoder[ProveDlog] = arrayBytesEncoder.contramap[ProveDlog](_.pkBytes)

  implicit val proveDlogDecoder: Decoder[ProveDlog] =
    arrayBytesDecoder.emap { xs =>
      Try(GroupElementSerializer.parse(xs)).toEither.leftMap(_.getMessage).map(ProveDlog(_))
    }

  implicit val assetEncoder: Encoder[(TokenId, Long)] =
    Encoder.instance { asset =>
      Json.obj(
        "tokenId" -> asset._1.asJson,
        "amount"  -> asset._2.asJson
      )
    }

  implicit val assetDecoder: Decoder[(TokenId, Long)] =
    Decoder.instance { cursor =>
      for {
        tokenId <- cursor.downField("tokenId").as[TokenId]
        amount  <- cursor.downField("amount").as[Long]
      } yield (tokenId, amount)
    }

  implicit val modifierIdEncoder: Encoder[ModifierId] =
    Encoder.instance(_.asInstanceOf[String].asJson)

  implicit val modifierIdDecoder: Decoder[ModifierId] = Decoder.instance(ModifierId @@ _.as[String])

  implicit val registerIdEncoder: KeyEncoder[NonMandatoryRegisterId] =
    KeyEncoder.instance { regId =>
      s"R${regId.number}"
    }

  implicit val registerIdDecoder: KeyDecoder[NonMandatoryRegisterId] =
    KeyDecoder.instance { key =>
      ErgoBox.registerByName.get(key).collect { case nonMandatoryId: NonMandatoryRegisterId =>
        nonMandatoryId
      }
    }

  implicit val evaluatedValueEncoder: Encoder[EvaluatedValue[_ <: SType]] =
    Encoder.instance { value =>
      ValueSerializer.serialize(value).asJson
    }

  implicit val evaluatedValueDecoder: Decoder[EvaluatedValue[_ <: SType]] =
    decodeEvaluatedValue(_.asInstanceOf[EvaluatedValue[SType]])

  def decodeEvaluatedValue[T](transform: EvaluatedValue[SType] => T): Decoder[T] =
    Decoder.instance { implicit cursor: ACursor =>
      cursor.as[Array[Byte]] flatMap { bytes =>
        fromThrows(
          transform(ValueSerializer.deserialize(bytes).asInstanceOf[EvaluatedValue[SType]])
        )
      }
    }

  implicit val dataInputEncoder: Encoder[DataInput] =
    Encoder.instance { input =>
      Json.obj(
        "boxId" -> input.boxId.asJson
      )
    }

  implicit val dataInputDecoder: Decoder[DataInput] =
    Decoder.instance { cursor =>
      for {
        boxId <- cursor.downField("boxId").as[ADKey]
      } yield DataInput(boxId)
    }

  implicit val inputEncoder: Encoder[Input] =
    Encoder.instance { input =>
      Json.obj(
        "boxId"         -> input.boxId.asJson,
        "spendingProof" -> input.spendingProof.asJson
      )
    }

  implicit val inputDecoder: Decoder[Input] =
    Decoder.instance { cursor =>
      for {
        boxId <- cursor.downField("boxId").as[ADKey]
        proof <- cursor.downField("spendingProof").as[ProverResult]
      } yield Input(boxId, proof)
    }

  implicit val unsignedInputEncoder: Encoder[UnsignedInput] =
    Encoder.instance { input =>
      Json.obj(
        "boxId"     -> input.boxId.asJson,
        "extension" -> input.extension.asJson
      )
    }

  implicit val unsignedInputDecoder: Decoder[UnsignedInput] =
    Decoder.instance { cursor =>
      for {
        boxId     <- cursor.downField("boxId").as[ADKey]
        extension <- cursor.downField("extension").as[ContextExtension]
      } yield new UnsignedInput(boxId, extension)
    }

  implicit val contextExtensionEncoder: Encoder[ContextExtension] =
    Encoder.instance { extension =>
      extension.values.map { case (key, value) =>
        key -> evaluatedValueEncoder(value)
      }.asJson
    }

  implicit val contextExtensionDecoder: Decoder[ContextExtension] =
    Decoder.instance { cursor =>
      for {
        values <- cursor.as[Map[Byte, EvaluatedValue[SType]]]
      } yield ContextExtension(values)
    }

  implicit val proverResultEncoder: Encoder[ProverResult] =
    Encoder.instance { v =>
      Json.obj(
        "proofBytes" -> v.proof.asJson,
        "extension"  -> v.extension.asJson
      )
    }

  implicit val proverResultDecoder: Decoder[ProverResult] =
    Decoder.instance { cursor =>
      for {
        proofBytes <- cursor.downField("proofBytes").as[Array[Byte]]
        extMap     <- cursor.downField("extension").as[Map[Byte, EvaluatedValue[SType]]]
      } yield ProverResult(proofBytes, ContextExtension(extMap))
    }

  implicit val avlTreeDataEncoder: Encoder[AvlTreeData] =
    Encoder.instance { v =>
      Json.obj(
        "digest"      -> v.digest.asJson,
        "treeFlags"   -> v.treeFlags.serializeToByte.asJson,
        "keyLength"   -> v.keyLength.asJson,
        "valueLength" -> v.valueLengthOpt.asJson
      )
    }

  implicit val avlTreeDataDecoder: Decoder[AvlTreeData] =
    Decoder.instance { cursor =>
      for {
        digest        <- cursor.downField("digest").as[ADDigest]
        treeFlagsByte <- cursor.downField("treeFlags").as[Byte]
        keyLength     <- cursor.downField("keyLength").as[Int]
        valueLength   <- cursor.downField("valueLength").as[Option[Int]]
      } yield new AvlTreeData(digest, AvlTreeFlags(treeFlagsByte), keyLength, valueLength)
    }

  implicit val ergoTreeEncoder: Encoder[ErgoTree] = Encoder.instance { value =>
    SigmaErgoTreeSerializer.DefaultSerializer.serializeErgoTree(value).asJson
  }

  def decodeErgoTree[T](transform: ErgoTree => T): Decoder[T] =
    Decoder.instance { implicit cursor: ACursor =>
      cursor.as[Array[Byte]] flatMap { bytes =>
        fromThrows(transform(SigmaErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(bytes)))
      }
    }

  implicit val ergoTreeDecoder: Decoder[ErgoTree] =
    decodeErgoTree(_.asInstanceOf[ErgoTree])

  implicit def registersEncoder[T <: EvaluatedValue[_ <: SType]]: Encoder[Map[NonMandatoryRegisterId, T]] =
    Encoder.instance { m =>
      Json.obj(
        m.toSeq
          .sortBy(_._1.number)
          .map { case (k, v) => registerIdEncoder(k) -> evaluatedValueEncoder(v) }: _*
      )
    }

  implicit val ergoBoxEncoder: Encoder[ErgoBox] =
    Encoder.instance { box =>
      Json.obj(
        "boxId"               -> box.id.asJson,
        "value"               -> box.value.asJson,
        "ergoTree"            -> SigmaErgoTreeSerializer.DefaultSerializer.serializeErgoTree(box.ergoTree).asJson,
        "assets"              -> box.additionalTokens.toArray.toSeq.asJson,
        "creationHeight"      -> box.creationHeight.asJson,
        "additionalRegisters" -> box.additionalRegisters.asJson,
        "transactionId"       -> box.transactionId.asJson,
        "index"               -> box.index.asJson
      )
    }

  implicit val ergoBoxDecoder: Decoder[ErgoBox] =
    Decoder.instance { cursor =>
      for {
        value            <- cursor.downField("value").as[Long]
        ergoTreeBytes    <- cursor.downField("ergoTree").as[Array[Byte]]
        additionalTokens <- cursor.downField("assets").as(Decoder.decodeSeq(assetDecoder))
        creationHeight   <- cursor.downField("creationHeight").as[Int]
        additionalRegisters <- cursor
                                 .downField("additionalRegisters")
                                 .as[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
        transactionId <- cursor.downField("transactionId").as[ModifierId]
        index         <- cursor.downField("index").as[Short]
      } yield new ErgoBox(
        value               = value,
        ergoTree            = SigmaErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(ergoTreeBytes),
        additionalTokens    = additionalTokens.toColl,
        additionalRegisters = additionalRegisters,
        transactionId       = transactionId,
        index               = index,
        creationHeight      = creationHeight
      )
    }

  implicit val ergoLikeTransactionEncoder: Encoder[ErgoLikeTransaction] =
    Encoder.instance { tx =>
      Json.obj(
        "id"         -> tx.id.asJson,
        "inputs"     -> tx.inputs.asJson,
        "dataInputs" -> tx.dataInputs.asJson,
        "outputs"    -> tx.outputs.asJson
      )
    }

  implicit val transactionOutputsDecoder: Decoder[(ErgoBoxCandidate, Option[BoxId])] =
    Decoder.instance { cursor =>
      for {
        maybeId        <- cursor.downField("boxId").as[Option[BoxId]]
        value          <- cursor.downField("value").as[Long]
        creationHeight <- cursor.downField("creationHeight").as[Int]
        ergoTree       <- cursor.downField("ergoTree").as[ErgoTree]
        assets <- cursor
                    .downField("assets")
                    .as[Seq[
                      (ErgoBox.TokenId, Long)
                    ]] // TODO optimize: encode directly into Coll avoiding allocation of Tuple2 for each element
        registers <- cursor
                       .downField("additionalRegisters")
                       .as[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
      } yield (
        new ErgoBoxCandidate(value, ergoTree, creationHeight, assets.toColl, registers),
        maybeId
      )
    }

  implicit val ergoLikeTransactionDecoder: Decoder[ErgoLikeTransaction] =
    Decoder.instance { implicit cursor =>
      for {
        inputs     <- cursor.downField("inputs").as[IndexedSeq[Input]]
        dataInputs <- cursor.downField("dataInputs").as[IndexedSeq[DataInput]]
        outputsWithIndex <- cursor
                              .downField("outputs")
                              .as[IndexedSeq[(ErgoBoxCandidate, Option[BoxId])]]
      } yield new ErgoLikeTransaction(inputs, dataInputs, outputsWithIndex.map(_._1))
    }

  implicit val unsignedErgoLikeTransactionDecoder: Decoder[UnsignedErgoLikeTransaction] =
    Decoder.instance { implicit cursor =>
      for {
        inputs     <- cursor.downField("inputs").as[IndexedSeq[UnsignedInput]]
        dataInputs <- cursor.downField("dataInputs").as[IndexedSeq[DataInput]]
        outputsWithIndex <- cursor
                              .downField("outputs")
                              .as[IndexedSeq[(ErgoBoxCandidate, Option[BoxId])]]
      } yield new UnsignedErgoLikeTransaction(inputs, dataInputs, outputsWithIndex.map(_._1))
    }

  implicit val ergoLikeTransactionTemplateDecoder: Decoder[ErgoLikeTransactionTemplate[_ <: UnsignedInput]] = {
    ergoLikeTransactionDecoder
      .asInstanceOf[Decoder[ErgoLikeTransactionTemplate[_ <: UnsignedInput]]] or
    unsignedErgoLikeTransactionDecoder
      .asInstanceOf[Decoder[ErgoLikeTransactionTemplate[_ <: UnsignedInput]]]
  }
}
