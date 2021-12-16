package org.ergoplatform.dex.domain

sealed trait ValueUnits[T]

case class CryptoUnits(asset: AssetClass) extends ValueUnits[AssetClass]
case class FiatUnits(currency: Currency) extends ValueUnits[Currency]
