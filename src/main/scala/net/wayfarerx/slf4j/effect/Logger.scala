/*
 * Logger.scala
 *
 * Copyright (c) 2019 wayfarerx.net <@thewayfarerx>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.wayfarerx.slf4j.effect

import org.slf4j

import zio.{Task, UIO, ZIO}
import zio.blocking.Blocking
import zio.console.Console

/**
 * Environment mix-in that exposes the logger API.
 */
trait Logger {

  /** The exposed logger API. */
  val logger: LoggerApi.Aux[Any]

}

/**
 * The global logger service and definitions that support the `Logger` environment.
 */
object Logger extends (LoggerApi.Aux[Any] => Logger) with LoggerApi.Service[Logger] {

  /**
   * Creates a new logger from the specified logger service.
   *
   * @param logger The logger service to use in the new logger.
   * @return A new logger from the specified logger service.
   */
  override def apply(logger: LoggerApi.Aux[Any]): Logger = {
    val _logger = logger
    new Logger {
      override val logger = _logger
    }
  }

  /* Return true if the specified logging level is enabled. */
  override def isEnabled(level: Level): Result[Boolean] =
    ZIO.accessM(_.logger.isEnabled(level))

  /* Attempt to submit a log event at the specified level using the supplied logging event data. */
  override def submit(
    level: Level,
    markers: Set[Marker],
    keyValuePairs: Map[String, AnyRef],
    message: => String,
    cause: Option[Throwable]
  ): Result[Unit] =
    ZIO.accessM(_.logger.submit(level, markers, keyValuePairs, message, cause))

  /**
   * Implementation of the `Logger` environment using a SLF4J `Logger`.
   */
  trait Live extends Logger {
    self: Blocking with Console =>

    /** The underlying SLF4J `Logger`. */
    val slf4jLogger: slf4j.Logger

    /* Implement the logger API. */
    final override val logger = new LoggerApi.Service[Any] {

      /* Return true if the specified level is enabled. */
      override def isEnabled(level: Level) = level match {
        case Level.Trace => UIO(slf4jLogger.isTraceEnabled)
        case Level.Debug => UIO(slf4jLogger.isDebugEnabled)
        case Level.Info => UIO(slf4jLogger.isInfoEnabled)
        case Level.Warn => UIO(slf4jLogger.isWarnEnabled)
        case Level.Error => UIO(slf4jLogger.isErrorEnabled)
      }

      /* Attempt to submit a log event at the specified level using the supplied logging event data. */
      override def submit(
        level: Level,
        markers: Set[Marker],
        keyValuePairs: Map[String, AnyRef],
        message: => String,
        cause: Option[Throwable]
      ) = for {
        enabled <- isEnabled(level)
        _ <- if (!enabled) UIO.unit else for {
          msg <- UIO(message)
          _ <- Task {
            val atLevel = level match {
              case Level.Trace => slf4jLogger.atTrace()
              case Level.Debug => slf4jLogger.atDebug()
              case Level.Info => slf4jLogger.atInfo()
              case Level.Warn => slf4jLogger.atWarn()
              case Level.Error => slf4jLogger.atError()
            }
            val withMarkers = markers.foldLeft(atLevel)((b, m) => b.addMarker(m.slf4jMarker))
            val withKeyValuePairs = keyValuePairs.foldLeft(withMarkers)((b, e) => b.addKeyValue(e._1, e._2))
            cause map withKeyValuePairs.setCause getOrElse withKeyValuePairs
          }.flatMap { builder =>
            blocking.effectBlocking(builder.log(msg))
          }.foldCauseM(
            failure => Recover(level.toString())(
              "Unable to submit SLF4J log entry:",
              4 -> (keyValuePairs.map(e => s"${e._1}=${e._2}").toSeq :+ msg mkString " "),
              4 -> cause,
              2 -> "SLF4J log entry submission prevented by:",
              4 -> failure
            ).provide(self),
            UIO(_)
          )
        } yield ()
      } yield ()

    }

  }

  /**
   * Factory for live `Logger` implementations.
   */
  object Live extends ((
    slf4j.Logger,
      Blocking.Service[Any],
      Console.Service[Any]
    ) => Live with Blocking with Console) {

    /**
     * Creates a live `Logger`.
     *
     * @param name The name of the logger.
     * @return A live `Logger`.
     */
    def apply(name: String): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getLogger(name)) map (Live(_))

    /**
     * Creates a live `Logger` with the specified blocking service.
     *
     * @param name     The name of the logger.
     * @param blocking The blocking service to use.
     * @return A live `Logger` with the specified blocking service.
     */
    def apply(name: String, blocking: Blocking.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getLogger(name)) map (Live(_, blocking))

    /**
     * Creates a live `Logger` with the specified console service.
     *
     * @param name    The name of the logger.
     * @param console The console service to use.
     * @return A live `Logger` with the specified console service.
     */
    def apply(name: String, console: Console.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getLogger(name)) map (Live(_, console = console))

    /**
     * Creates a live `Logger` with the specified blocking and console service.
     *
     * @param name     The name of the logger.
     * @param blocking The blocking service to use.
     * @param console  The console service to use.
     * @return A live `Logger` with the specified blocking and console service.
     */
    def apply(
      name: String,
      blocking: Blocking.Service[Any],
      console: Console.Service[Any]
    ): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getLogger(name)) map (Live(_, blocking, console))

    /**
     * Creates a new live `Logger` implementation.
     *
     * @param slf4jLogger The underlying SLF4J `Logger`.
     * @param blocking    The underlying blocking service, defaults to the global blocking service.
     * @param console     The underlying console service, defaults to the global console service.
     * @return A new live `Logger` implementation.
     */
    override def apply(
      slf4jLogger: slf4j.Logger,
      blocking: Blocking.Service[Any] = Blocking.Live.blocking,
      console: Console.Service[Any] = Console.Live.console
    ): Live with Blocking with Console = {
      val _slf4jLogger = slf4jLogger
      val _blocking = blocking
      val _console = console
      new Live with Blocking with Console {
        override val slf4jLogger = _slf4jLogger
        override val blocking = _blocking
        override val console = _console
      }
    }

  }

}
