name := """SecurePlayAPI"""
organization := "com.medincell"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.6"

libraryDependencies += guice
libraryDependencies += filters
libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-play-json" % "5.0.0",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.1",
  "com.auth0" % "jwks-rsa" % "0.6.1",
)
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test

fork in run := false