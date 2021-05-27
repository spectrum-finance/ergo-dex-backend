package org.ergoplatform.common

import io.estatico.newtype.macros.newtype
import pureconfig.ConfigReader

package object streaming {

  @newtype case class TopicId(value: String)

  object TopicId {
    implicit val configReader: ConfigReader[TopicId] = deriving
  }

  @newtype case class GroupId(value: String)

  object GroupId {
    implicit val configReader: ConfigReader[GroupId] = deriving
  }

  @newtype case class ClientId(value: String)

  object ClientId {
    implicit val configReader: ConfigReader[ClientId] = deriving
  }
}
