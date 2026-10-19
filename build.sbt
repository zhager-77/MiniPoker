ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "MiniPoker"
  )


Compile / run / fork := true

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.4",

  "org.http4s" %% "http4s-ember-server" % "0.23.27",
  "org.http4s" %% "http4s-dsl"          % "0.23.27",
  "org.http4s" %% "http4s-circe"        % "0.23.27",

  "io.circe" %% "circe-generic" % "0.14.9",
  "io.circe" %% "circe-core"    % "0.14.9",
  "io.circe" %% "circe-parser"  % "0.14.9",

  "co.fs2" %% "fs2-core" % "3.10.2",
  "co.fs2" %% "fs2-io"   % "3.10.2",

  "org.scalatest"     %% "scalatest"       % "3.2.19" % Test,
  "org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0" % Test,
  "org.typelevel"     %% "cats-effect-testkit" % "3.5.4" % Test,

  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,

  "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
  "ch.qos.logback" % "logback-classic" % "1.5.6" % Runtime,

  "org.tpolecat" %% "doobie-core" % "1.0.0-RC5",
  "org.tpolecat" %% "doobie-h2"   % "1.0.0-RC5",

  "com.github.pureconfig" %% "pureconfig-core" % "0.17.7"
)