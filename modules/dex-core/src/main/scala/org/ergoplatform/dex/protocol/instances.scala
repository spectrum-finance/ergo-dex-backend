package org.ergoplatform.dex.protocol

import cats.Show
import cats.instances.either._
import cats.instances.string._
import cats.syntax.either._
import doobie._
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.ergo.SErgoTree
import org.ergoplatform.common.HexString
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.serialization.{GroupElementSerializer, ErgoTreeSerializer => SigmaErgoTreeSerializer}
import tofu.logging.Loggable

object instances {

  implicit val ergoTreeGet: Get[ErgoTree] =
    Get[String].temap { s =>
      SErgoTree.fromString[Either[Throwable, *]](s).leftMap(_.getMessage) map
      (hex => ErgoTreeSerializer.default.deserialize(hex))
    }

  implicit val ergoTreePut: Put[ErgoTree] =
    Put[SErgoTree].contramap[ErgoTree](ErgoTreeSerializer.default.serialize)

  implicit val proveDlogGet: Get[ProveDlog] =
    Get[String].temap { s =>
      HexString.fromString[Either[Throwable, *]](s).leftMap(_.getMessage) map
      (hex => ProveDlog(GroupElementSerializer.fromBytes(Base16.decode(hex.unwrapped).get)))
    }

  implicit val proveDlogRead: Put[ProveDlog] =
    Put[String].contramap[ProveDlog](pk => Base16.encode(GroupElementSerializer.toBytes(pk.h)))

  implicit val ergoTreeShow: Show[ErgoTree] =
    tree => Base16.encode(SigmaErgoTreeSerializer.DefaultSerializer.serializeErgoTree(tree))

  implicit val proveDlogShow: Show[ProveDlog] =
    dlog => Base16.encode(dlog.pkBytes)

  implicit val ergoTreeLoggable: Loggable[ErgoTree] = Loggable.show

  implicit val proveDlogLoggable: Loggable[ProveDlog] = Loggable.show

  implicit val ergoLikeTxLoggable: Loggable[ErgoLikeTransaction] =
    Loggable.stringValue.contramap[ErgoLikeTransaction](_.toString) // todo:
}
