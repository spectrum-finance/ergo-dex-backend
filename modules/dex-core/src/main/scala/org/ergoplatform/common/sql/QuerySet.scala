package org.ergoplatform.common.sql

import doobie.Update
import doobie.util.Write

/** Database table access operations layer.
  */
trait QuerySet {

  /** Name of the table according to a database schema.
    */
  val tableName: String

  /** Table column names listing according to a database schema.
    */
  val fields: List[String]

  final def insert[M: Write]: Update[M] =
    Update[M](s"insert into $tableName ($fieldsString) values ($holdersString)")

  final def insertNoConflict[M: Write]: Update[M] =
    Update[M](s"insert into $tableName ($fieldsString) values ($holdersString) on conflict do nothing")

  private def fieldsString: String =
    fields.mkString(", ")

  private def holdersString: String =
    fields.map(_ => "?").mkString(", ")
}