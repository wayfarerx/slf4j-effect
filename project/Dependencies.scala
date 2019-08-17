import sbt._

object Dependencies {

  lazy val Slf4j = "org.slf4j" % "slf4j-api" % "1.7.28"

  lazy val Zio = "dev.zio" %% "zio" % "1.0.0-RC11-1"

  lazy val Logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  lazy val ScalaMock = "org.scalamock" %% "scalamock" % "4.3.0"

}
