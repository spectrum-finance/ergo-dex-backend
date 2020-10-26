import sbt.{CrossVersion, compilerPlugin, _}

object dependencies {

  import versions._

  val Http4s: List[ModuleID] =
    List(
      "org.http4s" %% "http4s-dsl",
      "org.http4s" %% "http4s-blaze-server",
      "org.http4s" %% "http4s-blaze-client",
      "org.http4s" %% "http4s-circe"
    ).map(_ % Http4sVersion)

  val Tapir: List[ModuleID] =
    List(
      "com.softwaremill.sttp.tapir" %% "tapir-core",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml"
    ).map(_ % TapirVersion)

  val Circe: List[ModuleID] =
    List(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser",
      "io.circe" %% "circe-refined"
    ).map(_       % CirceVersion) ++ List(
      "io.circe" %% "circe-derivation" % CirceDerivationVersion
    )

  val Cats: List[ModuleID] = List(
    "org.typelevel" %% "cats-core"           % CatsVersion,
    "org.typelevel" %% "cats-effect"         % CatsEffectVersion,
    "org.typelevel" %% "cats-mtl-core"       % CatsMtlVersion,
    "com.olegpy"    %% "meow-mtl-core"       % CatsMeowMtl,
    "org.typelevel" %% "cats-tagless-macros" % CatsTaglessVersion,
    "org.typelevel" %% "cats-tagless-core"   % CatsTaglessVersion,
    "org.typelevel" %% "mouse"               % MouseVersion
  )

  val Monix: List[ModuleID] = List("io.monix" %% "monix" % MonixVersion)

  val Monocle: List[ModuleID] = List(
    "com.github.julien-truffaut" %% "monocle-core"  % MonocleVersion,
    "com.github.julien-truffaut" %% "monocle-macro" % MonocleVersion
  )

  val Fs2: List[ModuleID] = List("co.fs2" %% "fs2-core" % Fs2Version)

  val Kafka: List[ModuleID] = List(
    "com.github.fd4s" %% "fs2-kafka" % Fs2KafkaVersion
  )

  val Tofu: List[ModuleID] = List(
    "ru.tinkoff"  %% "tofu-core"           % TofuVersion,
    "ru.tinkoff"  %% "tofu-concurrent"     % TofuVersion,
    "ru.tinkoff"  %% "tofu-optics-core"    % TofuVersion,
    "ru.tinkoff"  %% "tofu-optics-macro"   % TofuVersion,
    "ru.tinkoff"  %% "tofu-derivation"     % TofuVersion,
    "ru.tinkoff"  %% "tofu-logging"        % TofuVersion,
    "ru.tinkoff"  %% "tofu-doobie"         % TofuVersion,
    "ru.tinkoff"  %% "tofu-streams"        % TofuVersion,
    "ru.tinkoff"  %% "tofu-fs2-interop"    % TofuVersion,
    "org.manatki" %% "derevo-cats"         % DerevoVersion,
    "org.manatki" %% "derevo-cats-tagless" % DerevoVersion
  )

  val Ergo: List[ModuleID] = List(
    "org.ergoplatform" %% "ergo-wallet" % ErgoWalletVersion,
    "org.ergoplatform" %% "contracts"   % ErgoContractsVertions
  )

  val Db: List[ModuleID] = List(
    "org.tpolecat" %% "doobie-core"      % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres"  % DoobieVersion,
    "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari"    % DoobieVersion,
    "org.tpolecat" %% "doobie-refined"   % DoobieVersion,
    "org.flywaydb"  % "flyway-core"      % FlywayVersion
  )

  val Testing: List[ModuleID] = List(
    "org.tpolecat"               %% "doobie-scalatest"          % DoobieVersion                 % Test,
    "org.scalatest"              %% "scalatest"                 % ScalaTestVersion              % Test,
    "org.scalacheck"             %% "scalacheck"                % ScalaCheckVersion             % Test,
    "org.testcontainers"          % "postgresql"                % TestContainersPostgresVersion % Test,
    "com.dimafeng"               %% "testcontainers-scala"      % TestContainersScalaVersion    % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % ScalaCheckShapelessVersion    % Test
  )

  val Typing: List[ModuleID] = List(
    "org.scalaz"  %% "deriving-macro" % DerivingVersion,
    "io.estatico" %% "newtype"        % NewtypeVersion,
    "eu.timepit"  %% "refined"        % RefinedVersion,
    "eu.timepit"  %% "refined-cats"   % RefinedVersion
  )

  val Enums: List[ModuleID] = List(
    "com.beachape" %% "enumeratum"       % EnumeratumVersion,
    "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion
  )

  val Redis = List("dev.profunktor" %% "redis4cats-effects" % CatsRedisV)

  val Config: List[ModuleID] = List(
    "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion
  )

  val Simulacrum: List[ModuleID] = List(
    "com.github.mpilquist" %% "simulacrum" % SimulacrumVersion
  )

  val CompilerPlugins: List[ModuleID] =
    List(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % KindProjector cross CrossVersion.full
      ),
      compilerPlugin(
        "org.scalamacros" % "paradise" % MacroParadise cross CrossVersion.full
      ),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

  lazy val core: List[ModuleID] =
    Ergo ++
    Cats ++
    Tofu ++
    Fs2 ++
    Circe ++
    Typing ++
    Config ++
    Kafka ++
    Enums

  lazy val tracker: List[ModuleID] = Monix

  lazy val matcher: List[ModuleID] = Monix ++ Db

  lazy val executor: List[ModuleID] = Monix
}
