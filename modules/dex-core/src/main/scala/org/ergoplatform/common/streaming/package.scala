package org.ergoplatform.common

import derevo.derive
import io.estatico.newtype.macros.newtype
import pureconfig.ConfigReader
import tofu.logging.derivation.loggable

package object streaming {

  @derive(loggable)
  @newtype case class TopicId(value: String)

  object TopicId {
    implicit val configReader: ConfigReader[TopicId] = deriving
  }

  @derive(loggable)
  @newtype case class GroupId(value: String)

  object GroupId {
    implicit val configReader: ConfigReader[GroupId] = deriving
  }

  @derive(loggable)
  @newtype case class ClientId(value: String)

  object ClientId {
    implicit val configReader: ConfigReader[ClientId] = deriving
  }
}
