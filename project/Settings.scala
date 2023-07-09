import sbt.Keys.*
import scalafix.sbt.ScalafixPlugin.autoImport.scalafixOnCompile

object Settings {

  val commonSettings =
    Seq(
      scalacOptions     :=
        Seq("-unchecked", "-deprecation", "-feature"),
      run / cancelable  := true, // https://github.com/sbt/sbt/issues/2274
      scalafixOnCompile := true
    )
}
