package org.ergoplatform.dex

import io.estatico.newtype.macros.newtype

package object streaming {

  @newtype case class TopicId(value: String)

  @newtype case class GroupId(value: String)

  @newtype case class ClientId(value: String)
}
