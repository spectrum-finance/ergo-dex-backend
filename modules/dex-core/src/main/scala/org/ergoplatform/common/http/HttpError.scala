package org.ergoplatform.common.http

sealed trait HttpError
final case class NotFound(what: String) extends HttpError
final case class Unauthorized(realm: String) extends HttpError
final case class Unknown(code: Int, msg: String) extends HttpError
case object NoContent extends HttpError
