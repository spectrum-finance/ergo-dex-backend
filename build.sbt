import dependencies._

lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.14",
  organization := "org.ergoplatform",
  version := "0.2.0",
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case "logback.xml"                                => MergeStrategy.first
    case "module-info.class"                          => MergeStrategy.discard
    case "META-INF/intellij-compat.json"              => MergeStrategy.last
    case other if other.contains("io.netty.versions") => MergeStrategy.first
    case other if other.contains("scala")             => MergeStrategy.first
    case other if other.contains("derevo")            => MergeStrategy.last
    case other                                        => (assemblyMergeStrategy in assembly).value(other)
  },
  libraryDependencies ++= dependencies.Testing ++ dependencies.CompilerPlugins
)

lazy val commonScalacOptions = List(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ypartial-unification"
)

lazy val allConfigDependency = "compile->compile;test->test"

lazy val dexBackend = project
  .in(file("."))
  .withId("ergo-dex-backend")
  .settings(commonSettings)
  .settings(moduleName := "ergo-dex-backend", name := "ErgoDexBackend")
  .aggregate(core, utxoTracker, matcher, ordersExecutor, ammExecutor, poolResolver, marketsApi)

lazy val core = utils
  .mkModule("dex-core", "DexCore")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
      Cats ++
      Tofu ++
      Derevo ++
      Fs2 ++
      JawnFs2 ++
      Circe ++
      Typing ++
      Config ++
      Kafka ++
      Db ++
      SttpCore ++
      SttpClient ++
      Tapir ++
      Monocle ++
      Enums
  )

lazy val utxoTracker = utils
  .mkModule("utxo-tracker", "UtxoTracker")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.tracker.App"
    ),
    libraryDependencies ++= Monix
  )
  .dependsOn(core % allConfigDependency)

lazy val matcher = utils
  .mkModule("dex-matcher", "DexMatcher")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.matcher.App"
    ),
    libraryDependencies ++= Monix
  )
  .dependsOn(core % allConfigDependency)

lazy val ordersExecutor = utils
  .mkModule("orders-executor", "OrdersExecutor")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.executor.orders.App"
    ),
    libraryDependencies ++= Monix ++ SttpClient
  )
  .dependsOn(core % allConfigDependency)

lazy val ammExecutor = utils
  .mkModule("amm-executor", "AmmExecutor")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.executor.amm.App"
    ),
    libraryDependencies ++= Monix ++ SttpClient
  )
  .dependsOn(core % allConfigDependency)

lazy val poolResolver = utils
  .mkModule("pool-resolver", "PoolResolver")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.resolver.App"
    ),
    libraryDependencies ++= Monix ++ TapirHttp4s
  )
  .dependsOn(core % allConfigDependency)

lazy val marketsApi = utils
  .mkModule("markets-api", "MarketsApi")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.markets.App"
    ),
    libraryDependencies ++= Monix ++ Tapir
  )
  .dependsOn(core % allConfigDependency)
