import sbt.Keys.*
import sbt.Compile
import scalafix.sbt.ScalafixPlugin.autoImport.scalafixOnCompile

import Libs.*

object Settings {

  val bootSettings =
    Seq(
      run / cancelable    := true, // https://github.com/sbt/sbt/issues/2274
      run / fork          := true,
      run / connectInput  := true,
      Compile / mainClass := Some("zio.twitter.boot.Boot")
    )

  val commonSettings =
    Seq(
      scalacOptions     :=
        Seq("-unchecked", "-deprecation", "-feature"),
      scalafixOnCompile := true
    )

  val bootDependencies = zio :: Nil
}
