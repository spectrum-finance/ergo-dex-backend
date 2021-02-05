package org.ergoplatform.dex.configs

import cats.effect.Sync
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.module.catseffect._

import scala.reflect.ClassTag

trait ConfigBundleCompanion[T] {

  def load[F[_]: Sync](pathOpt: Option[String])(implicit
    r: ConfigReader[T],
    ct: ClassTag[T]
  ): F[T] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, T]
}
