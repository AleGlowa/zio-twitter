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
    .settings(coreSettings)
    .settings(bootSettings)

lazy val domain =
  project
    .dependsOn(commons)
    .settings(coreSettings)
    .settings(libraryDependencies ++= domainDependencies)

lazy val commons =
  project
    .settings(coreSettings)
    .settings(libraryDependencies ++= commonsDependencies)

lazy val root =
  (project in file("."))
    .aggregate(boot, domain, commons)

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt;scalafixAll")
addCommandAlias(
  "check",
  ";scalafmtCheckAll;scalafmtSbtCheck;scalafixAll --check"
)
