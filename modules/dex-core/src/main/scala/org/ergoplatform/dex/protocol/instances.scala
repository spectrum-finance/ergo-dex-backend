package org.ergoplatform.dex.protocol

import cats.Show
import doobie._
import cats.instances.either._
import cats.instances.string._
import cats.syntax.either._
import org.ergoplatform.dex.HexString
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.serialization.GroupElementSerializer
import sigmastate.serialization.{ErgoTreeSerializer => SigmaErgoTreeSerializer}
import tofu.logging.Loggable
import tofu.syntax.monadic._

object instances {

  implicit val ergoTreeGet: Get[ErgoTree] =
    Get[String].temap { s =>
      HexString.fromString[Either[Throwable, *]](s).leftMap(_.getMessage) >>=
        (hex => implicitly[ErgoTreeSerializer[Either[Throwable, *]]].deserialize(hex).leftMap(_.getMessage))
    }

  implicit val ergoTreePut: Put[ErgoTree] =
    Put[HexString].contramap[ErgoTree](implicitly[ErgoTreeSerializer[Either[Throwable, *]]].serialize)

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
}
