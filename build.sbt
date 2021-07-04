import dependencies._

lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.14",
  organization := "org.ergoplatform",
  version := "0.3.0",
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
  .aggregate(core, cache, db, http, utxoTracker, matcher, ordersExecutor, ammExecutor, poolResolver, marketsApi)

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
      TapirCore ++
      SttpCore ++
      SttpClient ++
      Monocle ++
      Enums
  )

lazy val cache = utils
  .mkModule("cache", "Cache")
  .settings(commonSettings, libraryDependencies ++= Redis ++ Scodec)
  .dependsOn(core % allConfigDependency)

lazy val db = utils
  .mkModule("db", "Db")
  .settings(commonSettings, libraryDependencies ++= Db)
  .dependsOn(core % allConfigDependency)

lazy val http = utils
  .mkModule("http", "Http")
  .settings(commonSettings, libraryDependencies ++= Tapir ++ TapirHttp4s)
  .dependsOn(core % allConfigDependency)

lazy val utxoTracker = utils
  .mkModule("utxo-tracker", "UtxoTracker")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.tracker.App"
    )
  )
  .settings(
    name in Universal := name.value,
    name in UniversalDocs := (name in Universal).value,
    name in UniversalSrc := (name in Universal).value,
    packageName in Universal := packageName.value,
    mappings in Universal += (packageBin in Compile map { p =>
      p -> "lib/utxo-tracker.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/utxo-tracker", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(core % allConfigDependency, cache % allConfigDependency)

lazy val matcher = utils
  .mkModule("dex-matcher", "DexMatcher")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.matcher.App"
    )
  )
  .settings(
    name in Universal := name.value,
    name in UniversalDocs := (name in Universal).value,
    name in UniversalSrc := (name in Universal).value,
    packageName in Universal := packageName.value,
    mappings in Universal += (packageBin in Compile map { p =>
      p -> "lib/dex-matcher.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/dex-matcher", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(core % allConfigDependency, db % allConfigDependency)

lazy val ordersExecutor = utils
  .mkModule("orders-executor", "OrdersExecutor")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.executor.orders.App"
    ),
    libraryDependencies ++= SttpClient
  )
  .settings(
    name in Universal := name.value,
    name in UniversalDocs := (name in Universal).value,
    name in UniversalSrc := (name in Universal).value,
    packageName in Universal := packageName.value,
    mappings in Universal += (packageBin in Compile map { p =>
      p -> "lib/orders-executor.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/orders-executor", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(core % allConfigDependency)

lazy val ammExecutor = utils
  .mkModule("amm-executor", "AmmExecutor")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.executor.amm.App"
    ),
    libraryDependencies ++= SttpClient
  )
  .settings(
    name in Universal := name.value,
    name in UniversalDocs := (name in Universal).value,
    name in UniversalSrc := (name in Universal).value,
    packageName in Universal := packageName.value,
    mappings in Universal += (packageBin in Compile map { p =>
      p -> "lib/amm-executor.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/amm-executor", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(core % allConfigDependency)

lazy val poolResolver = utils
  .mkModule("pool-resolver", "PoolResolver")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.resolver.App"
    )
  )
  .settings(
    name in Universal := name.value,
    name in UniversalDocs := (name in Universal).value,
    name in UniversalSrc := (name in Universal).value,
    packageName in Universal := packageName.value,
    mappings in Universal += (packageBin in Compile map { p =>
      p -> "lib/pool-resolver.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/pool-resolver", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(core % allConfigDependency, http % allConfigDependency)

lazy val marketsApi = utils
  .mkModule("markets-api", "MarketsApi")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some(
      "org.ergoplatform.dex.markets.App"
    ),
    libraryDependencies ++= Tapir
  )
  .settings(
    name in Universal := name.value,
    name in UniversalDocs := (name in Universal).value,
    name in UniversalSrc := (name in Universal).value,
    packageName in Universal := packageName.value,
    mappings in Universal += (packageBin in Compile map { p =>
      p -> "lib/markets-api.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/markets-api", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(core % allConfigDependency, db % allConfigDependency, http % allConfigDependency)
