import Settings.*

inThisBuild(
  Seq(
    name              := "zio-twitter",
    version           := "0.1",
    scalaVersion      := "3.3.0",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

lazy val root =
  (project in file("."))
    .settings(commonSettings)

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt;scalafixAll")
