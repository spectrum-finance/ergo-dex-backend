package org.ergoplatform.dex.index.repos

import cats.effect.Resource
import org.ergoplatform.dex.index.App._
import org.ergoplatform.dex.index.configs.ConfigBundle
import tofu.doobie.transactor.Txr

case class RepoBundle[F[_]](outputsRepo: OutputsRepo[F], assetsRepo: AssetsRepo[F], CFMMOrdersRepo: CFMMOrdersRepo[F])

object RepoBundle {

  def make(xa: Txr.Contextual[RunF, ConfigBundle]): Resource[Any, RepoBundle[xa.DB]] = {
    for {
      or <- Resource.eval(OutputsRepo.make[InitF, xa.DB])
      ar <- Resource.eval(AssetsRepo.make[InitF, xa.DB])
      cfmmr <- Resource.eval(CFMMOrdersRepo.make[InitF, xa.DB])
    } yield RepoBundle(or, ar, cfmmr)
  }

}