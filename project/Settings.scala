import sbt.Keys.*
import sbt.{Compile, TestFramework}
import scalafix.sbt.ScalafixPlugin.autoImport.scalafixOnCompile

object Settings {
  import Libs.*

  val bootSettings =
    Seq(
      run / cancelable    := true, // https://github.com/sbt/sbt/issues/2274
      run / fork          := true,
      run / connectInput  := true,
      Compile / mainClass := Some("zio.twitter.boot.Boot")
    )

  val coreSettings =
    Seq(
      scalacOptions     :=
        Seq("-unchecked", "-deprecation", "-feature"),
      scalafixOnCompile := true,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
      libraryDependencies ++= zio :: zioTest :: zioTestSbt :: Nil
    )

  val domainDependencies  = zioHttp :: zioPrelude :: zioJson :: Nil
  val commonsDependencies =
    zioLogging :: zioConfig :: zioConfigTypesafe :: zioHttp :: Nil
}
