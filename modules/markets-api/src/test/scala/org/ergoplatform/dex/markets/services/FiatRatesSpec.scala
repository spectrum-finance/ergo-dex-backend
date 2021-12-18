package org.ergoplatform.dex.markets.services

import cats.effect.{IO, IOApp}
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.services.FiatRates.ErgoOraclesRateSource
import org.ergoplatform.dex.protocol.constants.ErgoAssetClass
import org.ergoplatform.ergo.ErgoNetwork
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{SttpBackend, UriContext}
import tofu.logging.Logs
import tofu.{Context, WithContext}

class FiatRatesSpec extends AnyPropSpec with should.Matchers with CatsPlatform {

  implicit val conf: WithContext[IO, NetworkConfig] =
    Context.const(NetworkConfig(uri"https://api.ergoplatform.com", uri"http://127.0.0.1:9053"))

  implicit val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  property("Get ERG/USD rate") {
    val test = for {
      implicit0(back: SttpBackend[IO, Any]) <- AsyncHttpClientCatsBackend[IO]()
      network                               <- ErgoNetwork.make[IO, IO]
      rates = new ErgoOraclesRateSource(network)
      ergUsdRate <- rates.rateOf(ErgoAssetClass, UsdUnits)
      _          <- IO.delay(println(ergUsdRate))
    } yield ()
    test.unsafeRunSync()
  }
}
