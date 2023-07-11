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

lazy val boot =
  project
    .dependsOn(domain)
    .settings(bootSettings)
    .settings(libraryDependencies ++= commonDependencies)

lazy val domain =
  project
    .settings(commonSettings)
    .settings(libraryDependencies ++= commonDependencies)

lazy val root =
  (project in file("."))
    .aggregate(boot, domain)

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt;scalafixAll")
addCommandAlias(
  "check",
  ";scalafmtCheckAll;scalafmtSbtCheck;scalafixAll --check"
)
