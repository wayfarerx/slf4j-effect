import sbt._

object Dependencies {

  lazy val Slf4j = "org.slf4j" % "slf4j-api" % "2.0.0-alpha1"

  lazy val CatsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"

  lazy val Zio = "dev.zio" %% "zio" % "1.0.0-RC16"

  lazy val ZioCats = "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC7"

  lazy val Logback = "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4"

  lazy val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  lazy val ScalaMock = "org.scalamock" %% "scalamock" % "4.4.0"

  lazy val WayfarerxReleases = "WayfarerX Releases" at "https://software.wayfarerx.net/releases"

  lazy val WayfarerxSnapshots = "WayfarerX Snapshots" at "https://software.wayfarerx.net/snapshots"

  lazy val WayfarerxReleasesS3 = "WayfarerX S3 Releases" at "s3://software.wayfarerx.net/releases"

  lazy val WayfarerxSnapshotsS3 = "WayfarerX S3 Snapshots" at "s3://software.wayfarerx.net/snapshots"

}
