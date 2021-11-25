import dependencies._

lazy val NexusReleases  = "Sonatype Releases" at "https://s01.oss.sonatype.org/content/repositories/releases"
lazy val NexusSnapshots = "Sonatype Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.15",
  organization := "org.ergoplatform",
  version := "0.11.0",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("public"),
    Resolver.sonatypeRepo("snapshots"),
    NexusReleases,
    NexusSnapshots
  ),
  assembly / test := {},
  assembly / assemblyMergeStrategy := {
    case "logback.xml"                                => MergeStrategy.first
    case "module-info.class"                          => MergeStrategy.discard
    case "META-INF/intellij-compat.json"              => MergeStrategy.last
    case other if other.contains("io.netty.versions") => MergeStrategy.first
    case other if other.contains("scala")             => MergeStrategy.first
    case other if other.contains("derevo")            => MergeStrategy.last
    case other                                        => (assembly / assemblyMergeStrategy).value(other)
  },
  libraryDependencies ++= dependencies.Testing ++ dependencies.CompilerPlugins,
  ThisBuild / evictionErrorLevel := Level.Info
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
      Magnolia ++
      Fs2 ++
      JawnFs2 ++
      Circe ++
      Typing ++
      Config ++
      Kafka ++
      TapirCore ++
      SttpCore ++
      SttpClientFs2 ++
      ScodecCore ++
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
    assembly / mainClass := Some(
      "org.ergoplatform.dex.tracker.App"
    )
  )
  .settings(
    Universal / name := name.value,
    UniversalDocs / name := (Universal / name).value,
    UniversalSrc / name := (Universal / name).value,
    Universal / packageName := packageName.value,
    Universal / mappings += ((Compile / packageBin) map { p =>
      p -> "lib/utxo-tracker.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/utxo-tracker", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(Seq(core, cache).map(_ % allConfigDependency): _*)

lazy val matcher = utils
  .mkModule("dex-matcher", "DexMatcher")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some(
      "org.ergoplatform.dex.matcher.App"
    )
  )
  .settings(
    Universal / name := name.value,
    UniversalDocs / name := (Universal / name).value,
    UniversalSrc / name := (Universal / name).value,
    Universal / packageName := packageName.value,
    Universal / mappings += ((Compile / packageBin) map { p =>
      p -> "lib/dex-matcher.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/dex-matcher", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(Seq(core, db).map(_ % allConfigDependency): _*)

lazy val ordersExecutor = utils
  .mkModule("orders-executor", "OrdersExecutor")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some(
      "org.ergoplatform.dex.executor.orders.App"
    ),
    libraryDependencies ++= SttpClientCE
  )
  .settings(
    Universal / name := name.value,
    UniversalDocs / name := (Universal / name).value,
    UniversalSrc / name := (Universal / name).value,
    Universal / packageName := packageName.value,
    Universal / mappings += ((Compile / packageBin) map { p =>
      p -> "lib/orders-executor.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/orders-executor", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(Seq(core, http).map(_ % allConfigDependency): _*)

lazy val ammExecutor = utils
  .mkModule("amm-executor", "AmmExecutor")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some(
      "org.ergoplatform.dex.executor.amm.App"
    ),
    libraryDependencies ++= SttpClientCE
  )
  .settings(
    Universal / name := name.value,
    UniversalDocs / name := (Universal / name).value,
    UniversalSrc / name := (Universal / name).value,
    Universal / packageName := packageName.value,
    Universal / mappings += ((Compile / packageBin) map { p =>
      p -> "lib/amm-executor.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/amm-executor", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(Seq(core, http).map(_ % allConfigDependency): _*)

lazy val poolResolver = utils
  .mkModule("pool-resolver", "PoolResolver")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some("org.ergoplatform.dex.resolver.App"),
    libraryDependencies ++= RocksDB
  )
  .settings(
    Universal / name := name.value,
    UniversalDocs / name := (Universal / name).value,
    UniversalSrc / name := (Universal / name).value,
    Universal / packageName := packageName.value,
    Universal / mappings += ((Compile / packageBin) map { p =>
      p -> "lib/pool-resolver.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/pool-resolver", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(Seq(core, http).map(_ % allConfigDependency): _*)

lazy val marketsApi = utils
  .mkModule("markets-api", "MarketsApi")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some(
      "org.ergoplatform.dex.markets.App"
    ),
    libraryDependencies ++= Tapir
  )
  .settings(
    Universal / name := name.value,
    UniversalDocs / name := (Universal / name).value,
    UniversalSrc / name := (Universal / name).value,
    Universal / packageName := packageName.value,
    Universal / mappings += ((Compile / packageBin) map { p =>
      p -> "lib/markets-api.jar"
    }).value,
    dockerExposedVolumes := Seq("/var/lib/markets-api", "/opt/docker/logs/")
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)
  .dependsOn(Seq(core, db, http).map(_ % allConfigDependency): _*)
