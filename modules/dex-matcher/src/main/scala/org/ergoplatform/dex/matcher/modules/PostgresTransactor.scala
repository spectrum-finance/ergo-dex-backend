package org.ergoplatform.dex.matcher.modules

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.ergoplatform.dex.matcher.configs.DbConfig

object PostgresTransactor {

  def make[F[_]: Async: ContextShift](
    poolName: String,
    config: DbConfig
  ): Resource[F, HikariTransactor[F]] =
    for {
      cp      <- ExecutionContexts.fixedThreadPool(size = 16)
      blocker <- Blocker[F]
      xa <- HikariTransactor.newHikariTransactor[F](
              driverClassName = "org.postgresql.Driver",
              config.url,
              config.user,
              config.pass,
              cp,
              blocker
            )
      _ <- Resource.liftF(configure(xa)(poolName, config))
    } yield xa

  private def configure[F[_]: Sync](
    xa: HikariTransactor[F]
  )(name: String, config: DbConfig): F[Unit] =
    xa.configure { c =>
      Sync[F].delay {
        c.setAutoCommit(false)
        c.setPoolName(name)
        c.setMaxLifetime(600000)
        c.setIdleTimeout(30000)
        c.setMaximumPoolSize(config.maxConnections)
        c.setMinimumIdle(config.minConnections)
      }
    }
}
