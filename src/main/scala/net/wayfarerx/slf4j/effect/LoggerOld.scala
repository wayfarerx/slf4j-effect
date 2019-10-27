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

import java.util.{Collections => JavaCollections, HashMap => JavaHashMap}

import org.slf4j
import org.slf4j.spi.LoggingEventBuilder
import zio.{Task, UIO, ZIO}
import zio.blocking.Blocking
import zio.console.Console

/**
 * Environment mix-in that exposes the logger API.
 */
trait LoggerOld {

  /** The exposed logger API. */
  val logger: LoggerApi[Any]

}

/**
 * The global logger service and definitions that support the `Logger` environment.
 */
object LoggerOld extends (LoggerApi[Any] => LoggerOld) with LoggerApi.Service[LoggerOld] {

  /**
   * Creates a new logger from the specified logger service.
   *
   * @param logger The logger service to use in the new logger.
   * @return A new logger from the specified logger service.
   */
  override def apply(logger: LoggerApi[Any]): LoggerOld = {
    val _logger = logger
    new LoggerOld {
      override val logger = _logger
    }
  }

  /* Return true if the specified logging level is enabled. */
  override def isEnabled(level: Level): Result[Boolean] =
    ZIO.accessM(_.logger.isEnabled(level))

  /* Return true if the specified logging level is enabled with the supplied marker. */
  override def isEnabled(level: Level, marker: Marker): Result[Boolean] =
    ZIO.accessM(_.logger.isEnabled(level, marker))

  /* Attempt to submit a log event at the specified level using the supplied logging event data. */
  override def submit(
    level: Level,
    markers: Set[Marker],
    keyValuePairs: Map[String, AnyRef],
    message: UIO[String],
    cause: Option[Throwable]
  ): Result[Unit] =
    ZIO.accessM(_.logger.submit(level, markers, keyValuePairs, message, cause))

  /**
   * Implementation of the `Logger` environment using a SLF4J `Logger`.
   */
  trait Live extends LoggerOld {
    self: Blocking with Console =>

    /** The underlying SLF4J `Logger`. */
    val slf4jLogger: slf4j.Logger

    /* Implement the logger API. */
    final override val logger: LoggerApi[Any] = new LoggerApi.Service[Any] {

      /* Return true if the specified level is enabled. */
      override def isEnabled(level: Level) = level match {
        case Level.Trace => UIO(slf4jLogger.isTraceEnabled)
        case Level.Debug => UIO(slf4jLogger.isDebugEnabled)
        case Level.Info => UIO(slf4jLogger.isInfoEnabled)
        case Level.Warn => UIO(slf4jLogger.isWarnEnabled)
        case Level.Error => UIO(slf4jLogger.isErrorEnabled)
      }

      /* Return true if the specified level is enabled with the supplied marker. */
      override def isEnabled(level: Level, maker: Marker) = level match {
        case Level.Trace => UIO(slf4jLogger.isTraceEnabled(maker.slf4jMarker))
        case Level.Debug => UIO(slf4jLogger.isDebugEnabled(maker.slf4jMarker))
        case Level.Info => UIO(slf4jLogger.isInfoEnabled(maker.slf4jMarker))
        case Level.Warn => UIO(slf4jLogger.isWarnEnabled(maker.slf4jMarker))
        case Level.Error => UIO(slf4jLogger.isErrorEnabled(maker.slf4jMarker))
      }

      /* Attempt to submit a log event at the specified level using the supplied logging event data. */
      override def submit(
        level: Level,
        markers: Set[Marker],
        keyValuePairs: Map[String, AnyRef],
        message: UIO[String],
        cause: Option[Throwable]
      ) = {

        def builder: LoggingEventBuilder = {
          val result = keyValuePairs.foldLeft {
            markers.foldLeft {
              level match {
                case Level.Trace => slf4jLogger.atTrace()
                case Level.Debug => slf4jLogger.atDebug()
                case Level.Info => slf4jLogger.atInfo()
                case Level.Warn => slf4jLogger.atWarn()
                case Level.Error => slf4jLogger.atError()
              }
            }(_ addMarker _.slf4jMarker)
          }((b, e) => b.addKeyValue(e._1, e._2))
          cause map result.setCause getOrElse result
        }

        for {
          enabled <- if (markers.size == 1) isEnabled(level, markers.head) else isEnabled(level)
          _ <- if (!enabled) UIO.unit else for {
            msg <- message
            mdc <- MDC()
            _ <- blocking.effectBlocking {
              if (mdc.isEmpty) builder.log(msg) else {
                val parent = Option(slf4j.MDC.getCopyOfContextMap) getOrElse JavaCollections.emptyMap()
                val child = new JavaHashMap[String, String](parent)
                mdc foreach (child.put _).tupled
                slf4j.MDC.setContextMap(child)
                try builder.log(msg) finally slf4j.MDC.setContextMap(parent)
              }
            }.foldCauseM(
              failure => Recover(level.toString())(
                "Failed while submitting SLF4J log entry:",
                4 -> (keyValuePairs.map(e => s"${e._1}=${e._2}").toSeq :+ msg mkString " "),
                4 -> cause,
                2 -> "SLF4J log entry submission encountered failure:",
                4 -> failure
              ).provide(slf4jEffectRuntime.Environment), // FIXME
              _ => UIO.unit
            )
          } yield ()
        } yield ()
      }

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
