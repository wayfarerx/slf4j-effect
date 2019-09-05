/*
 * LoggerApi.scala
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

import zio.{Cause, URIO}

/**
 * Definition of the API for accessing SLF4J loggers.
 *
 * @tparam R The environment type to require in results.
 */
trait LoggerApi[-R] {

  import LoggerApi.EventBuilder

  /** The type of result returned by this API. */
  type Result[+A] <: URIO[R, A]

  /**
   * Returns true if the `TRACE` logging level is enabled.
   *
   * @return A result that returns true if the `TRACE` logging level is enabled.
   */
  final def isTraceEnabled: Result[Boolean] = isEnabled(Level.Trace)

  /**
   * Prepares to log a message at the `TRACE` logging level.
   *
   * @return A logging event builder at the `TRACE` logging level.
   */
  final def trace: EventBuilder[R] = log(Level.Trace)

  /**
   * Returns true if the `DEBUG` logging level is enabled.
   *
   * @return A result that returns true if the `DEBUG` logging level is enabled.
   */
  final def isDebugEnabled: Result[Boolean] = isEnabled(Level.Debug)

  /**
   * Prepares to log a message at the `DEBUG` logging level.
   *
   * @return A logging event builder at the `DEBUG` logging level.
   */
  final def debug: EventBuilder[R] = log(Level.Debug)

  /**
   * Returns true if the `INFO` logging level is enabled.
   *
   * @return A result that returns true if the `INFO` logging level is enabled.
   */
  final def isInfoEnabled: Result[Boolean] = isEnabled(Level.Info)

  /**
   * Prepares to log a message at the `INFO` logging level.
   *
   * @return A logging event builder at the `INFO` logging level.
   */
  final def info: EventBuilder[R] = log(Level.Info)

  /**
   * Returns true if the `WARN` logging level is enabled.
   *
   * @return A result that returns true if the `WARN` logging level is enabled.
   */
  final def isWarnEnabled: Result[Boolean] = isEnabled(Level.Warn)

  /**
   * Prepares to log a message at the `WARN` logging level.
   *
   * @return A logging event builder at the `WARN` logging level.
   */
  final def warn: EventBuilder[R] = log(Level.Warn)

  /**
   * Returns true if the `ERROR` logging level is enabled.
   *
   * @return A result that returns true if the `ERROR` logging level is enabled.
   */
  final def isErrorEnabled: Result[Boolean] = isEnabled(Level.Error)

  /**
   * Prepares to log a message at the `ERROR` logging level.
   *
   * @return A logging event builder at the `ERROR` logging level.
   */
  final def error: EventBuilder[R] = log(Level.Error)

  /**
   * Returns true if the specified logging level is enabled.
   *
   * @param level The logging level to check the status of.
   * @return A result that returns true if the specified logging level is enabled.
   */
  def isEnabled(level: Level): Result[Boolean]

  /**
   * Prepares to log a message at the specified level.
   *
   * @param level The level to log the message at.
   * @return A logging event builder at the specified level.
   */
  final def log(level: Level): EventBuilder[R] = EventBuilder(this, level)

  /**
   * Attempts to submit a log event at the specified level using the supplied logging event data.
   *
   * @param level         The level to log the message at.
   * @param keyValuePairs The key/value pairs to use for the logging event.
   * @param message       The message to log.
   * @param cause         The optional cause of the resulting logging event.
   * @return A result that attempts to submit a log event at the specified level
   */
  def submit(
    level: Level,
    keyValuePairs: Map[String, AnyRef],
    message: => String,
    cause: Option[Throwable]
  ): Result[Unit]

}

/**
 * Definitions that support the `LoggerApi` trait.
 */
object LoggerApi {

  /** The type of logger APIs with a fixed environment. */
  type Aux[R] = LoggerApi[R] {type Result[+A] = URIO[R, A]}

  /**
   * Base type for logger API implementations.
   *
   * @tparam R The environment type to require in results.
   */
  trait Service[R] extends LoggerApi[R] {

    /* Configure the type of result returned by this API. */
    final override type Result[+A] = URIO[R, A]

  }

  /**
   * A wrapper around a `LoggerApi` that collects the data about a logging event.
   *
   * @tparam R The environment type to require in results.
   * @param loggerApi     The `LoggerApi` to use.
   * @param level         The level this builder will log at.
   * @param keyValuePairs The key/value pairs to use for the logging event.
   */
  case class EventBuilder[-R](
    loggerApi: LoggerApi[R],
    level: Level,
    keyValuePairs: Map[String, AnyRef] = Map.empty
  ) {

    import EventBuilder._

    /**
     * Returns this event builder with the specified component(s) appended.
     *
     * @param components The component(s) to append.
     * @return This event builder with the specified component(s) appended.
     */
    def apply(components: EventComponent*): EventBuilder[R] =
      components.foldLeft(this)((builder, component) => component(builder))

    /**
     * Submits this event builder with the specified message.
     *
     * @param message A function that returns the log message.
     * @return The result of submitting this event builder with the specified message.
     */
    def apply(message: => String): URIO[R, Unit] =
      loggerApi.submit(level, keyValuePairs, message, None)

    /**
     * Submits this event builder with the specified message and cause.
     *
     * @tparam C The type of cause that inspired this log entry.
     * @param message A function that returns the log message.
     * @param cause   The cause to record with the logging event.
     * @return The result of submitting this event builder with the specified message and cause.
     */
    def apply[C: CauseSupport](message: => String, cause: C): URIO[R, Unit] =
      loggerApi.submit(level, keyValuePairs, message, Some(CauseSupport(cause)))

    /**
     * Submits this event builder with the specified message and optional cause.
     *
     * @tparam C The type of cause that inspired this log entry.
     * @param message A function that returns the log message.
     * @param cause   The optional cause to record with the logging event.
     * @return The result of submitting this event builder with the specified message and optional cause.
     */
    def apply[C: CauseSupport](message: => String, cause: Option[C]): URIO[R, Unit] =
      loggerApi.submit(level, keyValuePairs, message, cause map (CauseSupport(_)))

  }

  /**
   * Definitions that support the `EventBuilder` type.
   */
  object EventBuilder {

    /**
     * Base type for objects that capture event components.
     */
    trait EventComponent {

      /**
       * Applies this event component to the specified event builder.
       *
       * @tparam R The type required by the builder.
       * @param builder The builder to apply this event component to.
       * @return The specified event builder with this event component applied to it.
       */
      def apply[R](builder: EventBuilder[R]): EventBuilder[R]

    }

    /**
     * Definitions of the supported event component types.
     */
    object EventComponent {

      /** The singleton key/value pair event component implementation. */
      implicit def keyValuePairAsEventComponent[T: ValueSupport](component: (String, T)): EventComponent =
        new EventComponent {
          override def apply[R](builder: EventBuilder[R]) =
            builder.copy(keyValuePairs = builder.keyValuePairs + (component._1 -> ValueSupport(component._2)))
        }

    }

    /**
     * A type class that converts a value of the underlying type into a reference.
     *
     * @tparam T The underlying type to convert into a reference.
     */
    trait ValueSupport[-T] extends (T => AnyRef)

    /**
     * Definition of the supported values.
     */
    object ValueSupport {

      /** Support for all booleans as values. */
      implicit val booleans: ValueSupport[Boolean] = java.lang.Boolean.valueOf

      /** Support for all bytes as values. */
      implicit val bytes: ValueSupport[Byte] = java.lang.Byte.valueOf

      /** Support for all shorts as values. */
      implicit val shorts: ValueSupport[Short] = java.lang.Short.valueOf

      /** Support for all integers as values. */
      implicit val ints: ValueSupport[Int] = java.lang.Integer.valueOf

      /** Support for all longs as values. */
      implicit val longs: ValueSupport[Long] = java.lang.Long.valueOf

      /** Support for all floats as values. */
      implicit val floats: ValueSupport[Float] = java.lang.Float.valueOf

      /** Support for all doubles as values. */
      implicit val doubles: ValueSupport[Double] = java.lang.Double.valueOf

      /** Support for all characters as values. */
      implicit val chars: ValueSupport[Char] = java.lang.Character.valueOf

      /** Support for all references as values. */
      implicit val references: ValueSupport[AnyRef] = identity(_)

      /**
       * Returns the implicit value support for the specified type.
       *
       * @tparam T The type to return the implicit value support for.
       * @return The implicit value support for the specified type.
       */
      def apply[T: ValueSupport](): ValueSupport[T] = implicitly[ValueSupport[T]]

      /**
       * Converts the specified value into a reference.
       *
       * @tparam T The type of value to convert into a reference.
       * @param value The value to convert into a reference.
       * @return The specified value converted into a reference.
       */
      def apply[T: ValueSupport](value: T): AnyRef = apply[T]().apply(value)

    }

    /**
     * A type class converts a cause of the underlying type into a throwable.
     *
     * @tparam T The underlying type to convert into a throwable.
     */
    sealed trait CauseSupport[-T] extends (T => Throwable)

    /**
     * Common cause adapter implementations.
     */
    object CauseSupport {

      /** Support for all throwables as causes. */
      implicit val throwableSupport: CauseSupport[Throwable] = new CauseSupport[Throwable] {
        override def apply(thrown: Throwable) = thrown
      }

      /** Support for all throwable causes as causes. */
      implicit val throwableCauseSupport: CauseSupport[Cause[Throwable]] = new CauseSupport[Cause[Throwable]] {
        override def apply(cause: Cause[Throwable]) = cause.squash
      }

      /**
       * Returns the cause support type class for the specified type.
       *
       * @tparam T The type to return the cause support type class for.
       * @return The cause support type class for the specified type.
       */
      def apply[T: CauseSupport](): CauseSupport[T] = implicitly[CauseSupport[T]]

      /**
       * Converts the specified cause into a throwable.
       *
       * @tparam T The type of cause to convert into a throwable.
       * @param cause The cause to convert into a throwable.
       * @return The specified cause converted into a throwable.
       */
      def apply[T: CauseSupport](cause: T): Throwable = apply[T]().apply(cause)

    }

  }

}
