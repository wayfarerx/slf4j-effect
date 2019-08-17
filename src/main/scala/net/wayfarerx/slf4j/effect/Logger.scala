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

import java.io.{PrintWriter, StringWriter}

import org.slf4j

import zio.{Cause, Task, UIO}
import zio.blocking.Blocking
import zio.console.Console

/**
 * Environment mix-in that directly exposes the Logger API.
 */
trait Logger {

  /** The provided logger service. */
  def logger: LoggerApi[Any]

}

/**
 * Definitions that support the `Logger` mix-in.
 */
object Logger {

  /**
   * Attempts to create a new `Logger` implementation.
   *
   * @tparam I The type of input that describes the underlying SLF4J `Logger`.
   * @param input    The input that describes the underlying SLF4J `Logger`.
   * @param blocking The underlying blocking service.
   * @param console  The underlying console service.
   * @return An effect that creates a new `Logger` implementation.
   */
  def apply[I: Factory](
    input: I,
    blocking: Blocking.Service[Any] = Blocking.Live.blocking,
    console: Console.Service[Any] = Console.Live.console
  ): Task[Logger with Blocking with Console] =
    implicitly[Factory[I]].apply(input, blocking) map (Live(_, blocking, console))

  /**
   * Implementation of the `Logger` mix-in using a SLF4J `Logger`.
   */
  trait Live extends Logger {
    self: Blocking with Console =>

    /** The underlying SLF4J `Logger`. */
    val slf4jLogger: slf4j.Logger

    /* Implement the logger API. */
    final override val logger: LoggerApi[Any] = new LoggerApi[Any] {

      /* This API's effects are unbound. */
      override type Effect[+A] = UIO[A]

      /* Return true if the specified level is enabled. */
      override def isEnabled(level: Level) = UIO {
        level match {
          case Level.Error => slf4jLogger.isErrorEnabled
          case Level.Warn => slf4jLogger.isWarnEnabled
          case Level.Info => slf4jLogger.isInfoEnabled
          case Level.Debug => slf4jLogger.isDebugEnabled
          case Level.Trace => slf4jLogger.isTraceEnabled
        }
      }

      /* Log a message at the specified level. */
      override def log(level: Level, f: => String) =
        isEnabled(level) flatMap (if (_) UIO(f) flatMap (submit(level, _, None)) else UIO.unit)

      /* Log a message and `Throwable` at the specified level. */
      override def log(level: Level, f: => String, t: Throwable) =
        isEnabled(level) flatMap (if (_) UIO(f) flatMap (submit(level, _, Some(t))) else UIO.unit)

      /**
       * Submits a log entry to SLF4J, falling back to sysout if necessary.
       *
       * @param level   The level of the log entry.
       * @param message The message for the log entry.
       * @param thrown  The throwable for the log entry.
       * @return An effect that submits a log entry to SLF4J.
       */
      private def submit(level: Level, message: String, thrown: Option[Throwable]): UIO[Unit] = {

        /* Generate a multi-line string that describes the logging failure. */
        def reportLogFailure(cause: Cause[Throwable]): String = {
          val result = new StringWriter()
          val out = new PrintWriter(result)

          /* Print the specified lines with the level prefix and indent. */
          def printLines(lines: String): Unit =
            lines.linesWithSeparators map (_.stripLineEnd) filterNot (_.trim.isEmpty) foreach { line =>
              out.println(s"$level   $line")
            }

          out.println(s"$level Unable to submit log entry:")
          printLines(message)
          thrown foreach { t =>
            val sw = new StringWriter()
            val pw = new PrintWriter(sw)
            t.printStackTrace(pw)
            pw.flush()
            printLines(sw.toString)
          }
          out.println(s"$level Log entry submission prevented by:")
          printLines(cause.prettyPrint)
          out.flush()
          result.toString
        }

        blocking.effectBlocking {
          level match {
            case Level.Error => thrown.fold(slf4jLogger.error(message))(slf4jLogger.error(message, _))
            case Level.Warn => thrown.fold(slf4jLogger.warn(message))(slf4jLogger.warn(message, _))
            case Level.Info => thrown.fold(slf4jLogger.info(message))(slf4jLogger.info(message, _))
            case Level.Debug => thrown.fold(slf4jLogger.debug(message))(slf4jLogger.debug(message, _))
            case Level.Trace => thrown.fold(slf4jLogger.trace(message))(slf4jLogger.trace(message, _))
          }
        }.foldCauseM(c => console.putStr(reportLogFailure(c)), UIO(_))
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

  /**
   * Base type for supported SLF4J `Logger` factories.
   *
   * @tparam I The type of input that is supported.
   */
  trait Factory[I] {

    /**
     * Attempts to create a SLF4J `Logger`.
     *
     * @param input    The input to create from.
     * @param blocking The blocking service to use.
     * @return An effect that attempts to create a SLF4J `Logger`.
     */
    def apply(input: I, blocking: Blocking.Service[Any]): Task[slf4j.Logger]

  }

  /**
   * Definitions of the supported SLF4J `Logger` factories.
   */
  object Factory {

    /** Use the specified SLF4J `Logger`. */
    implicit val fromSlf4jLogger: Factory[slf4j.Logger] =
      (logger, _) => UIO(logger)

    /** Create a SLF4J `Logger` with the specified name. */
    implicit val fromSlf4jLoggerName: Factory[String] =
      (loggerName, blocking) => blocking.effectBlocking(slf4j.LoggerFactory.getLogger(loggerName))

    /** Create a SLF4J `Logger` from the specified class. */
    implicit def fromSlf4jLoggerClass[T]: Factory[Class[T]] =
      (loggerClass, blocking) => blocking.effectBlocking(slf4j.LoggerFactory.getLogger(loggerClass))

  }

}
