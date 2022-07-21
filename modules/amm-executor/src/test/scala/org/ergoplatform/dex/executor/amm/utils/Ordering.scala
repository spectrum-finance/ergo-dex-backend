package org.ergoplatform.dex.executor.amm.utils

import scala.math.Ordering.Implicits.infixOrderingOps

object Ordering {

  def checkDescSort[A: Ordering](in: List[A]): Boolean =
    (in.headOption, in.tail.headOption) match {
      case (Some(head), Some(nextAfterHead)) if head >= nextAfterHead =>
        checkDescSort(in.tail)
      case (Some(_), None) => true
      case _               => false
    }
}
