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

import zio.{UIO, ZIO}
import zio.blocking.Blocking
import zio.console.Console

/**
 * Environment mix-in that exposes the logger API.
 */
trait Logger {

  /** The exposed logger API. */
  def logger: LoggerApi.Aux[Any]

}

/**
 * The global logger service and definitions that support the `Logger` environment.
 */
object Logger extends LoggerApi.Service[Logger] {

  /* Return true if the specified logging level is enabled. */
  override def isEnabled(level: Level): Result[Boolean] =
    ZIO.accessM(_.logger.isEnabled(level))

  /* Log a message and an optional `Throwable` at the specified level. */
  override def log(level: Level, f: => String, t: Option[Throwable]): Result[Unit] =
    ZIO.accessM(_.logger.log(level, f, t))

  /**
   * Implementation of the `Logger` environment using a SLF4J `Logger`.
   */
  trait Live extends Logger {
    self: Blocking with Console =>

    /** The underlying SLF4J `Logger`. */
    val slf4jLogger: slf4j.Logger

    /** The mapping of logging levels to effects that check those levels' status. */
    private val enabled = Map[Level, UIO[Boolean]](
      Level.Trace -> UIO(slf4jLogger.isTraceEnabled),
      Level.Debug -> UIO(slf4jLogger.isDebugEnabled),
      Level.Info -> UIO(slf4jLogger.isInfoEnabled),
      Level.Warn -> UIO(slf4jLogger.isWarnEnabled),
      Level.Error -> UIO(slf4jLogger.isErrorEnabled)
    )

    /* Implement the logger API. */
    final override val logger = new LoggerApi.Service[Any] {

      /* Return true if the specified level is enabled. */
      override def isEnabled(level: Level) = enabled(level)

      /* Log a message and optional `Throwable` at the specified level. */
      override def log(level: Level, f: => String, t: Option[Throwable]) = for {
        continue <- enabled(level)
        _ <- if (!continue) UIO.unit else for {
          msg <- UIO(f)
          _ <- blocking.effectBlocking {
            level match {
              case Level.Error => t.fold(slf4jLogger.error(msg))(slf4jLogger.error(msg, _))
              case Level.Warn => t.fold(slf4jLogger.warn(msg))(slf4jLogger.warn(msg, _))
              case Level.Info => t.fold(slf4jLogger.info(msg))(slf4jLogger.info(msg, _))
              case Level.Debug => t.fold(slf4jLogger.debug(msg))(slf4jLogger.debug(msg, _))
              case Level.Trace => t.fold(slf4jLogger.trace(msg))(slf4jLogger.trace(msg, _))
            }
          }.foldCauseM(cause => Recover(level.toString())(
            "Unable to submit log entry:",
            4 -> msg,
            4 -> t,
            2 -> "Log entry submission prevented by:",
            4 -> cause
          ).provide(self), UIO(_))
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
