import Dependencies._

resolvers ++= Seq(WayfarerxReleases, WayfarerxSnapshots)

lazy val scala2_12 = "2.12.9"
lazy val scala2_13 = "2.13.0"

scalaVersion in ThisBuild := scala2_12
organization in ThisBuild := "net.wayfarerx"
name in ThisBuild := "slf4j-effect"

crossScalaVersions := List(scala2_12, scala2_13)
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, n)) if n >= 13 => Seq("-Xsource:2.14")
  case _ => Seq()
})

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)

libraryDependencies ++= Seq(Slf4j, CatsEffect, Zio, ZioCats, ScalaTest % Test, ScalaMock % Test, Logback % Test)

coverageMinimum := 100
//coverageFailOnMinimum := true

publishMavenStyle := true
publishTo := {
  if (isSnapshot.value) Some(WayfarerxSnapshotsS3) else Some(WayfarerxReleasesS3)
}