import Dependencies._

organization := "net.wayfarerx"

name := "slf4j-effect"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ypartial-unification")

libraryDependencies ++= Seq(Slf4j, Zio, ScalaTest % Test, ScalaMock % Test)

coverageMinimum := 100
coverageFailOnMinimum := true