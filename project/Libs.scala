import sbt.*

object Libs {
  import Versions.*

  val zio        = "dev.zio" %% "zio"          % zioV
  val zioHttp    = "dev.zio" %% "zio-http"     % zioHttpV
  val zioPrelude = "dev.zio" %% "zio-prelude"  % zioPreludeV
  val zioJson    = "dev.zio" %% "zio-json"     % zioJsonV
  val zioTest    = "dev.zio" %% "zio-test"     % zioV % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioV % Test
}

object Versions {

  val zioV        = "2.0.15"
  val zioHttpV    = "3.0.0-RC2"
  val zioPreludeV = "1.0.0-RC19"
  val zioJsonV    = "0.6.0"
}
