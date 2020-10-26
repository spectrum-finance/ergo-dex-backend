package org.ergoplatform.dex.protocol

import doobie._
import cats.instances.either._
import cats.instances.string._
import cats.syntax.either._
import org.ergoplatform.dex.HexString
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.serialization.GroupElementSerializer
import tofu.syntax.monadic._

object instances {

  implicit def ergoTreeGet: Get[ErgoTree] =
    Get[String].temap { s =>
      HexString.fromString[Either[Throwable, *]](s).leftMap(_.getMessage) >>=
        (hex => implicitly[ErgoTreeSerializer[Either[Throwable, *]]].deserialize(hex).leftMap(_.getMessage))
    }

  implicit def ergoTreePut: Put[ErgoTree] =
    Put[HexString].contramap[ErgoTree](implicitly[ErgoTreeSerializer[Either[Throwable, *]]].serialize)

  implicit def proveDlogGet: Get[ProveDlog] =
    Get[String].temap { s =>
      HexString.fromString[Either[Throwable, *]](s).leftMap(_.getMessage) map
      (hex => ProveDlog(GroupElementSerializer.fromBytes(Base16.decode(hex.unwrapped).get)))
    }

  implicit def proveDlogRead: Put[ProveDlog] =
    Put[String].contramap[ProveDlog](pk => Base16.encode(GroupElementSerializer.toBytes(pk.h)))
}
