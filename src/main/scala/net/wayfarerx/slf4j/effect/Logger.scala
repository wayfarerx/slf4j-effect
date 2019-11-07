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

import language.{higherKinds, implicitConversions}

import cats.Monad
import cats.implicits._
import cats.effect.{Async, Effect}

import org.slf4j

import zio.{Cause, RIO, Runtime, Task, UIO, URIO}
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz._

/**
 * Definition of the API for using SLF4J loggers.
 *
 * @tparam F The type of effect that this logger uses.
 */
trait Logger[F[_]] {

  import Logger._

  //
  // Trace-level API.
  //

  /**
   * Returns true if the TRACE logging level is enabled.
   *
   * @return True if the TRACE logging level is enabled.
   */
  @inline final def traceEnabled(): F[Boolean] = enabled(Level.Trace)

  /**
   * Returns true if the TRACE logging level is enabled with the specified marker.
   *
   * @param marker The marker to check the status of.
   * @return True if the TRACE logging level is enabled with the specified marker.
   */
  @inline final def traceEnabled(marker: Marker): F[Boolean] = enabled(Level.Trace, marker)

  /**
   * Returns true if the TRACE logging level is enabled with the specified optional marker.
   *
   * @param marker The optional marker to check the status of.
   * @return True if the TRACE logging level is enabled with the specified optional marker.
   */
  @inline final def traceEnabled(marker: Option[Marker]): F[Boolean] = enabled(Level.Trace, marker)

  /**
   * Submits a logging entry at the TRACE level.
   *
   * @param entry A function that constructs a logging event from a logging event builder.
   * @return The result of submitting a logging entry at the TRACE level.
   */
  @inline final def trace(entry: Entry): F[Unit] = log(Level.Trace, entry)

  /**
   * Submits a logging event at the TRACE level with the supplied message.
   *
   * @param message The desired log message.
   * @return A logging event at the TRACE level with the supplied message.
   */
  @inline final def trace(message: => String): F[Unit] = log(Level.Trace, message)

  //
  // Other APIs.
  //

  /**
   * Returns true if the debug logging level is enabled.
   *
   * @return True if the debug logging level is enabled.
   */
  @inline final def debugEnabled(): F[Boolean] = enabled(Level.Debug)

  /**
   * Returns true if the debug logging level is enabled with the supplied marker.
   *
   * @param marker The marker to check the status of.
   * @return True if the debug logging level is enabled with the supplied marker.
   */
  @inline final def debugEnabled(marker: Marker): F[Boolean] = enabled(Level.Debug, marker)

  /**
   * Returns true if the info logging level is enabled.
   *
   * @return True if the info logging level is enabled.
   */
  @inline final def infoEnabled(): F[Boolean] = enabled(Level.Info)

  /**
   * Returns true if the info logging level is enabled with the supplied marker.
   *
   * @param marker The marker to check the status of.
   * @return True if the info logging level is enabled with the supplied marker.
   */
  @inline final def infoEnabled(marker: Marker): F[Boolean] = enabled(Level.Info, marker)

  /**
   * Returns true if the warn logging level is enabled.
   *
   * @return True if the warn logging level is enabled.
   */
  @inline final def warnEnabled(): F[Boolean] = enabled(Level.Warn)

  /**
   * Returns true if the warn logging level is enabled with the supplied marker.
   *
   * @param marker The marker to check the status of.
   * @return True if the warn logging level is enabled with the supplied marker.
   */
  @inline final def warnEnabled(marker: Marker): F[Boolean] = enabled(Level.Warn, marker)

  /**
   * Returns true if the error logging level is enabled.
   *
   * @return True if the error logging level is enabled.
   */
  @inline final def errorEnabled(): F[Boolean] = enabled(Level.Error)

  /**
   * Returns true if the error logging level is enabled with the supplied marker.
   *
   * @param marker The marker to check the status of.
   * @return True if the error logging level is enabled with the supplied marker.
   */
  @inline final def errorEnabled(marker: Marker): F[Boolean] = enabled(Level.Error, marker)

  /**
   * Submits a logging event at the debug level using the specified logging event builder.
   *
   * @param f A function that constructs a logging event.
   * @return A logging event at the debug level using the specified logging event builder.
   */
  @inline final def debug(f: Builder => Event): F[Unit] = log(Level.Debug, f)

  /**
   * Submits a logging event at the info level using the specified logging event builder.
   *
   * @param f A function that constructs a logging event.
   * @return A logging event at the info level using the specified logging event builder.
   */
  @inline final def info(f: Builder => Event): F[Unit] = log(Level.Info, f)

  /**
   * Submits a logging event at the warn level using the specified logging event builder.
   *
   * @param f A function that constructs a logging event.
   * @return A logging event at the warn level using the specified logging event builder.
   */
  @inline final def warn(f: Builder => Event): F[Unit] = log(Level.Warn, f)

  /**
   * Submits a logging event at the error level using the specified logging event builder.
   *
   * @param f A function that constructs a logging event.
   * @return A logging event at the error level using the specified logging event builder.
   */
  @inline final def error(f: Builder => Event): F[Unit] = log(Level.Error, f)

  /**
   * Submits a logging event at the warn level with the supplied message.
   *
   * @param message A function that returns the log message.
   * @return A logging event at the debug level with the supplied message.
   */
  @inline final def debug(message: => String): F[Unit] =
    log(Level.Warn, _ (message))

  /**
   * Submits a logging event at the info level with the supplied message.
   *
   * @param message A function that returns the log message.
   * @return A logging event at the info level with the supplied message.
   */
  @inline final def info(message: => String): F[Unit] =
    log(Level.Info, _ (message))

  /**
   * Submits a logging event at the specified level with the supplied message.
   *
   * @param message A function that returns the log message.
   * @return A logging event at the warn level with the supplied message.
   */
  @inline final def warn(message: => String): F[Unit] =
    log(Level.Warn, _ (message))

  /**
   * Submits a logging event at the specified level with the supplied message.
   *
   * @param message A function that returns the log message.
   * @return A logging event at the error level with the supplied message.
   */
  @inline final def error(message: => String): F[Unit] =
    log(Level.Error, _ (message))

  //
  // Level-agnostic API.
  //

  /**
   * Returns true if the specified logging level is enabled.
   *
   * @param level The logging level to check the status of.
   * @return True if the specified logging level is enabled.
   */
  def enabled(level: Level): F[Boolean]

  /**
   * Returns true if the specified logging level is enabled with the supplied marker.
   *
   * @param level  The logging level to check the status of.
   * @param marker The marker to check the status of.
   * @return True if the specified logging level is enabled with the supplied marker.
   */
  def enabled(level: Level, marker: Marker): F[Boolean]

  /**
   * Returns true if the specified logging level is enabled with the supplied optional marker.
   *
   * @param level  The logging level to check the status of.
   * @param marker The optional marker to check the status of.
   * @return True if the specified logging level is enabled with the supplied optional marker.
   */
  final def enabled(level: Level, marker: Option[Marker]): F[Boolean] =
    marker map (enabled(level, _)) getOrElse enabled(level)

  /**
   * Submits a logging entry at the specified level.
   *
   * @param level The level to log the event at.
   * @param entry A function that constructs a logging event from a logging event builder.
   * @return The result of submitting a logging entry at the specified level.
   */
  def log(level: Level, entry: Entry): F[Unit]

  /**
   * Submits a logging event at the specified level with the supplied message.
   *
   * @param level   The level to log the event at.
   * @param message The desired log message.
   * @return The result of submitting a logging entry at the specified level with the supplied message.
   */
  final def log(level: Level, message: => String): F[Unit] =
    log(level, _ (message))

  /**
   * Submits a logging event at the specified level with the supplied message and problem.
   *
   * @tparam P The type of problem to submit.
   * @param level   The level to log the event at.
   * @param message The desired log message.
   * @param problem The problem to submit.
   * @return The result of submitting a logging entry at the specified level with the supplied message and problem.
   */
  final def log[P: Problem](level: Level, message: => String, problem: P): F[Unit] =
    log(level, _ (message, problem))

  /**
   * Submits a logging event at the specified level with the supplied message and optional problem.
   *
   * @tparam P The type of problem to submit.
   * @param level   The level to log the event at.
   * @param message The desired log message.
   * @param problem The optional problem to submit.
   * @return The result of submitting a logging entry at the specified level with the supplied message and problem.
   */
  final def log[P: Problem](level: Level, message: => String, problem: Option[P]): F[Unit] =
    problem map (log(level, message, _)) getOrElse log(level, message)

}

/**
 * The global SLF4J logger service and definitions that support the `Logger` type.
 */
object Logger {

  /** A logging entry that builds a logging event. */
  type Entry = Builder => Event

  /** The environment required to submit logging events. */
  type Environment = Clock with Console

  /** A logging event that can be submitted to SLF4J. */
  type Event = (Builder, String, Option[Throwable])

  /**
   * Creates a ZIO logger that wraps the specified SLF4J logger.
   *
   * @param slf4jLogger The SLF4J logger to wrap.
   * @return A ZIO logger that wraps the specified SLF4J logger.
   */
  def apply(slf4jLogger: slf4j.Logger): Service[URIO[Environment, *]] = {
    val _slf4jLogger = slf4jLogger
    new Service[URIO[Environment, *]] {

      final override def monad = implicitly[Monad[URIO[Environment, *]]]

      final override def slf4jLogger = _slf4jLogger

      final override protected def lift[A](action: URIO[Environment, A]) = action

    }
  }

  /**
   * Creates a ZIO logger that wraps the specified SLF4J logger with the supplied environment.
   *
   * @param slf4jLogger The SLF4J logger to wrap.
   * @param env The environment to execute SLF4J operations in.
   * @return A ZIO logger that wraps the specified SLF4J logger with the supplied environment.
   */
  def apply(slf4jLogger: slf4j.Logger, env: Environment): Service[UIO] = {
    val _slf4jLogger = slf4jLogger
    new Service[UIO] {

      final override def monad = implicitly[Monad[UIO]]

      final override def slf4jLogger = _slf4jLogger

      final override protected def lift[A](action: URIO[Environment, A]) = action provide env

    }
  }

  /**
   * Creates a cats-effect logger that wraps the specified SLF4J logger.
   *
   * @tparam F The type of effect that the logger will use.
   * @param slf4jLogger The SLF4J logger to wrap.
   * @param runtime     The optional ZIO runtime to use.
   * @return A cats-effect logger that wraps the specified SLF4J logger.
   */
  def using[F[_] : Async](slf4jLogger: slf4j.Logger, runtime: Runtime[Environment] = slf4jEffectRuntime): Service[F] = {
    val _slf4jLogger = slf4jLogger
    implicit val _runtime: Runtime[Environment] = runtime
    val rio = implicitly[Effect[RIO[Environment, *]]]
    new Service[F] {

      final override def monad = implicitly[Async[F]]

      final override def slf4jLogger = _slf4jLogger

      final override protected def lift[A](action: URIO[Environment, A]) = monad.liftIO(rio.toIO(action))

    }
  }

  /**
   * Base class for monadic `Logger` implementations on top of the SLF4J API.
   *
   * @tparam F The type of effect that this logger uses.
   */
  trait Service[F[_]] extends Logger[F] {

    /** The monad that describes the effect type. */
    implicit def monad: Monad[F]

    /** The underlying SLF4J logger to use. */
    def slf4jLogger: slf4j.Logger

    /* Evaluate to true if the specified logging level is enabled. */
    final override def enabled(level: Level): F[Boolean] = monad.pure {
      level match {
        case Level.Trace => slf4jLogger.isTraceEnabled
        case Level.Debug => slf4jLogger.isDebugEnabled
        case Level.Info => slf4jLogger.isInfoEnabled
        case Level.Warn => slf4jLogger.isWarnEnabled
        case Level.Error => slf4jLogger.isErrorEnabled
      }
    }

    /* Evaluate to true if the specified logging level is enabled with the supplied marker. */
    final override def enabled(level: Level, marker: Marker): F[Boolean] = monad.pure {
      level match {
        case Level.Trace => slf4jLogger.isTraceEnabled(marker.slf4jMarker)
        case Level.Debug => slf4jLogger.isDebugEnabled(marker.slf4jMarker)
        case Level.Info => slf4jLogger.isInfoEnabled(marker.slf4jMarker)
        case Level.Warn => slf4jLogger.isWarnEnabled(marker.slf4jMarker)
        case Level.Error => slf4jLogger.isErrorEnabled(marker.slf4jMarker)
      }
    }

    /* Submit a logging entry at the specified level. */
    final override def log(level: Level, entry: Entry): F[Unit] = for {
      continue <- enabled(level)
      _ <- if (!continue) monad.unit else lift {
        val (builder, message, problem) = entry(Builder())
        Task {
          val result = builder.keyValuePairs.foldLeft {
            builder.markers.foldLeft {
              level match {
                case Level.Trace => slf4jLogger.atTrace()
                case Level.Debug => slf4jLogger.atDebug()
                case Level.Info => slf4jLogger.atInfo()
                case Level.Warn => slf4jLogger.atWarn()
                case Level.Error => slf4jLogger.atError()
              }
            }(_ addMarker _.slf4jMarker)
          }((b, kv) => b.addKeyValue(kv._1, kv._2))
          problem map result.setCause getOrElse result log message
        }.foldCauseM(
          failure => Recover(level.toString())(
            "Failed while submitting SLF4J log entry:",
            4 -> (
              builder.markers.toSeq.map(m => s"@$m") ++
                builder.keyValuePairs.toSeq.map(e => s"${e._1}=${e._2}") :+
                message mkString " "
              ),
            4 -> problem,
            2 -> "SLF4J log entry submission encountered failure:",
            4 -> failure
          ),
          _ => UIO.unit
        )
      }
    } yield ()

    /**
     * Lift an environmentally-dependant ZIO effect into the underlying monadic type.
     *
     * @tparam A The type of result produced by the ZIO effect.
     * @param action The ZIO action to lift into the monadic type.
     * @return The specified ZIO effect lifted into the underlying monadic type.
     */
    protected def lift[A](action: URIO[Environment, A]): F[A]

  }

  /**
   * A utility that collects information about and ultimately builds a logging event.
   *
   * @param markers       The markers to set on the logging event.
   * @param keyValuePairs The key/value pairs to set on the logging event.
   */
  case class Builder(markers: Set[Marker] = Set.empty, keyValuePairs: Map[String, AnyRef] = Map.empty) {

    /**
     * Returns this event builder with the specified metadata appended.
     *
     * @param metadata The metadata to append.
     * @return This event builder with the specified metadata appended.
     */
    def apply(metadata: Metadata[_]*): Builder = metadata.foldLeft(this)((builder, datum) => datum(builder))

    /**
     * Submits this event builder with the specified log message.
     *
     * @param message The log message to submit.
     * @return The result of submitting this event builder with the specified log message.
     */
    def apply(message: String): Event = (this, message, None)

    /**
     * Submits this event builder with the specified log message and problem.
     *
     * @tparam P The type of problem to submit.
     * @param message The log message to submit.
     * @param problem The problem to submit.
     * @return The result of submitting this event builder with the specified log message and problem.
     */
    def apply[P: Problem](message: String, problem: P): Event = (this, message, Some(Problem(problem)))

    /**
     * Submits this event builder with the specified message and optional problem.
     *
     * @tparam P The type of problem to submit.
     * @param message The log message to submit.
     * @param problem The optional problem to submit.
     * @return The result of submitting this event builder with the specified message and optional problem.
     */
    def apply[P: Problem](message: String, problem: Option[P]): Event = (this, message, problem map Problem[P])

  }

  /**
   * A piece of metadata paired with its adapter.
   *
   * @tparam T The underlying type to apply to an event builder.
   */
  case class Metadata[T: Adapter](data: T) {

    /**
     * Applies the underlying metadata to the specified event builder.
     *
     * @param builder The builder to apply the underlying metadata to.
     * @return The specified event builder with the underlying metadata applied to it.
     */
    @inline def apply(builder: Builder): Builder = Adapter(builder, data)

  }

  /**
   * The implicit metadata factory.
   */
  object Metadata {

    /**
     * Implicit support for all metadata types with an implicit metadata adapter.
     *
     * @tparam T The underlying type to apply to an event builder.
     * @param data The metadata to apply to the builder.
     * @return Implicit support for all metadata types with an implicit metadata adapter.
     */
    @inline implicit def asMetadata[T: Adapter](data: T): Metadata[T] = Metadata(data)

  }

  /**
   * A type class that applies an instance of the underlying type to an event builder.
   *
   * @tparam T The underlying type to apply to an event builder.
   */
  sealed trait Adapter[-T] extends ((Builder, T) => Builder)

  /**
   * The supported metadata adapter implementations.
   */
  object Adapter {

    /** Support for all markers. */
    implicit lazy val markers: Adapter[Marker] = new Adapter[Marker] {
      override def apply(b: Builder, m: Marker) =
        b.copy(markers = b.markers + m)
    }

    /** Support for all references. */
    implicit lazy val keysWithReferences: Adapter[(String, AnyRef)] = new Adapter[(String, AnyRef)] {
      override def apply(b: Builder, e: (String, AnyRef)) =
        b.copy(keyValuePairs = b.keyValuePairs + e)
    }

    /** Support for all booleans. */
    implicit lazy val keysWithBooleans: Adapter[(String, Boolean)] = new Adapter[(String, Boolean)] {
      override def apply(b: Builder, e: (String, Boolean)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Boolean.valueOf(e._2)))
    }

    /** Support for all bytes. */
    implicit lazy val keysWithBytes: Adapter[(String, Byte)] = new Adapter[(String, Byte)] {
      override def apply(b: Builder, e: (String, Byte)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Byte.valueOf(e._2)))
    }

    /** Support for all shorts. */
    implicit lazy val keysWithShorts: Adapter[(String, Short)] = new Adapter[(String, Short)] {
      override def apply(b: Builder, e: (String, Short)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Short.valueOf(e._2)))
    }

    /** Support for all integers. */
    implicit lazy val keysWithIntegers: Adapter[(String, Int)] = new Adapter[(String, Int)] {
      override def apply(b: Builder, e: (String, Int)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Integer.valueOf(e._2)))
    }

    /** Support for all longs. */
    implicit lazy val keysWithLongs: Adapter[(String, Long)] = new Adapter[(String, Long)] {
      override def apply(b: Builder, e: (String, Long)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Long.valueOf(e._2)))
    }

    /** Support for all floats. */
    implicit lazy val keysWithFloats: Adapter[(String, Float)] = new Adapter[(String, Float)] {
      override def apply(b: Builder, e: (String, Float)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Float.valueOf(e._2)))
    }

    /** Support for all doubles. */
    implicit lazy val keysWithDoubles: Adapter[(String, Double)] = new Adapter[(String, Double)] {
      override def apply(b: Builder, e: (String, Double)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Double.valueOf(e._2)))
    }

    /** Support for all characters. */
    implicit lazy val keysWithChars: Adapter[(String, Char)] = new Adapter[(String, Char)] {
      override def apply(b: Builder, e: (String, Char)) =
        b.copy(keyValuePairs = b.keyValuePairs + (e._1 -> java.lang.Character.valueOf(e._2)))
    }

    /**
     * Returns the implicit metadata adapter type class for the specified type.
     *
     * @tparam T The type to return the implicit metadata adapter type class for.
     * @return The implicit metadata adapter type class for the specified type.
     */
    @inline def apply[T: Adapter]: Adapter[T] = implicitly[Adapter[T]]

    /**
     * Applies the supplied metadata to the specified event builder.
     *
     * @tparam F The type required by the builder.
     * @tparam T The type of data to apply to the builder.
     * @param builder The builder to apply the metadata to.
     * @param data    The metadata to apply to the builder.
     * @return The specified event builder with the supplied metadata applied to it.
     */
    @inline def apply[F[_], T: Adapter](builder: Builder, data: T): Builder = Adapter[T].apply(builder, data)

  }

  /**
   * A type class converts an instance of the underlying type into a throwable.
   *
   * @tparam T The underlying type to convert into a throwable.
   */
  trait Problem[-T] extends (T => Throwable)

  /**
   * The supported problem implementations.
   */
  object Problem {

    /** Support for all throwable problems. */
    implicit lazy val throwables: Problem[Throwable] = identity(_)

    /** Support for all throwable cause problems. */
    implicit lazy val causes: Problem[Cause[Throwable]] = _.squash

    /**
     * Returns the implicit problem type class for the specified type.
     *
     * @tparam T The type to return the implicit problem type class for.
     * @return The implicit problem type class for the specified type.
     */
    @inline def apply[T: Problem]: Problem[T] = implicitly[Problem[T]]

    /**
     * Converts the specified problem into a throwable using the implicit problem type class.
     *
     * @tparam T The type of problem to convert into a throwable using the implicit problem type class.
     * @param problem The cause to convert into a throwable using the implicit problem type class.
     * @return The specified cause converted into a throwable using the implicit problem type class.
     */
    @inline def apply[T: Problem](problem: T): Throwable = Problem[T].apply(problem)

  }

}
