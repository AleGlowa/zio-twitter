import sbt.*

object Libs {
  import Versions.*

  val zio = "dev.zio" %% "zio" % zioV
}

object Versions {

  val zioV = "2.0.15"
}
