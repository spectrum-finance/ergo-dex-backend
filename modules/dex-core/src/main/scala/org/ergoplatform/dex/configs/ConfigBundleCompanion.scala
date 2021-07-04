package org.ergoplatform.dex.configs

import cats.effect.{Blocker, ContextShift, Sync}
import pureconfig.module.catseffect.syntax._
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

trait ConfigBundleCompanion[T] {

  def load[F[_]: Sync: ContextShift](pathOpt: Option[String], blocker: Blocker)(implicit
    r: ConfigReader[T],
    ct: ClassTag[T]
  ): F[T] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, T](blocker)
}
