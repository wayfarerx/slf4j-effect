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
import zio.{Cause, UIO, URIO}

/**
 * Definition of the API for using SLF4J loggers.
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
   * @return A result that evaluates to true if the `TRACE` logging level is enabled.
   */
  final def isTraceEnabled: Result[Boolean] = isEnabled(Level.Trace)

  /**
   * Prepares to log a message at the `TRACE` logging level.
   *
   * @return A logging event builder set to the `TRACE` logging level.
   */
  final def trace: EventBuilder[R] = log(Level.Trace)

  /**
   * Returns true if the `DEBUG` logging level is enabled.
   *
   * @return A result that evaluates to true if the `DEBUG` logging level is enabled.
   */
  final def isDebugEnabled: Result[Boolean] = isEnabled(Level.Debug)

  /**
   * Prepares to log a message at the `DEBUG` logging level.
   *
   * @return A logging event builder set to the `DEBUG` logging level.
   */
  final def debug: EventBuilder[R] = log(Level.Debug)

  /**
   * Returns true if the `INFO` logging level is enabled.
   *
   * @return A result that evaluates to true if the `INFO` logging level is enabled.
   */
  final def isInfoEnabled: Result[Boolean] = isEnabled(Level.Info)

  /**
   * Prepares to log a message at the `INFO` logging level.
   *
   * @return A logging event builder set to the `INFO` logging level.
   */
  final def info: EventBuilder[R] = log(Level.Info)

  /**
   * Returns true if the `WARN` logging level is enabled.
   *
   * @return A result that evaluates to true if the `WARN` logging level is enabled.
   */
  final def isWarnEnabled: Result[Boolean] = isEnabled(Level.Warn)

  /**
   * Prepares to log a message at the `WARN` logging level.
   *
   * @return A logging event builder set to the `WARN` logging level.
   */
  final def warn: EventBuilder[R] = log(Level.Warn)

  /**
   * Returns true if the `ERROR` logging level is enabled.
   *
   * @return A result that evaluates to true if the `ERROR` logging level is enabled.
   */
  final def isErrorEnabled: Result[Boolean] = isEnabled(Level.Error)

  /**
   * Prepares to log a message at the `ERROR` logging level.
   *
   * @return A logging event builder set to the `ERROR` logging level.
   */
  final def error: EventBuilder[R] = log(Level.Error)

  /**
   * Returns true if the specified logging level is enabled.
   *
   * @param level The logging level to check the status of.
   * @return A result that evaluates to true if the specified logging level is enabled.
   */
  def isEnabled(level: Level): Result[Boolean]

  /**
   * Returns true if the specified logging level is enabled with the supplied marker.
   *
   * @param level  The logging level to check the status of.
   * @param marker The marker to check the status of.
   * @return A result that evaluates to true if the specified logging level is enabled with the supplied marker.
   */
  def isEnabled(level: Level, marker: Marker): Result[Boolean]

  /**
   * Prepares to log a message at the specified level.
   *
   * @param level The level to log the message at.
   * @return A logging event builder set to the specified level.
   */
  final def log(level: Level): EventBuilder[R] = EventBuilder(this, level)

  /**
   * Submits a logging event at the specified level using the supplied logging event data.
   *
   * @param level         The level to log the message at.
   * @param markers       The markers to use for the logging event.
   * @param keyValuePairs The key/value pairs to use for the logging event.
   * @param message       The message to log.
   * @param cause         The optional cause of the resulting logging event.
   * @return A result that attempts to submit a logging event at the specified level
   */
  def submit(
    level: Level,
    markers: Set[Marker],
    keyValuePairs: Map[String, AnyRef],
    message: UIO[String],
    cause: Option[Throwable]
  ): Result[Unit]

}

/**
 * Definitions that support the `LoggerApi` trait.
 */
object LoggerApi {

  /**
   * Base type for logger API implementations with a fixed environment type.
   *
   * @tparam R The environment type to require in results.
   */
  trait Service[R] extends LoggerApi[R] {

    /* Configure the type of result returned by this API. */
    final override type Result[+A] = URIO[R, A]

  }

  /**
   * A wrapper around a logger API that collects information about a logging event.
   *
   * @tparam R The environment type to require in results.
   * @param loggerApi     The logger API to wrap.
   * @param level         The level to use when the logging the event.
   * @param markers       The markers to supply when the logging the event.
   * @param keyValuePairs The key/value pairs to supply when the logging the event.
   */
  case class EventBuilder[-R](
    loggerApi: LoggerApi[R],
    level: Level,
    markers: Set[Marker] = Set(),
    keyValuePairs: Map[String, AnyRef] = Map.empty
  ) {

    import EventBuilder._

    /**
     * Returns this event builder with the specified metadata appended.
     *
     * @param metadata The metadata to append.
     * @return This event builder with the specified metadata appended.
     */
    def apply(metadata: Metadata[_]*): EventBuilder[R] =
      metadata.foldLeft(this)((builder, data) => data(builder))

    /**
     * Submits this event builder with the specified message.
     *
     * @param message A function that returns the log message.
     * @return The result of submitting this event builder with the specified message.
     */
    def apply(message: => String): URIO[R, Unit] =
      apply(UIO(message))

    /**
     * Submits this event builder with the specified message and cause.
     *
     * @tparam C The type of cause that inspired this log entry.
     * @param message A function that returns the log message.
     * @param cause   The cause to record with the logging event.
     * @return The result of submitting this event builder with the specified message and cause.
     */
    def apply[C: ThrowableAdapter](message: => String, cause: C): URIO[R, Unit] =
      apply(UIO(message), cause)

    /**
     * Submits this event builder with the specified message and optional cause.
     *
     * @tparam C The type of cause that inspired this log entry.
     * @param message A function that returns the log message.
     * @param cause   The optional cause to record with the logging event.
     * @return The result of submitting this event builder with the specified message and optional cause.
     */
    def apply[C: ThrowableAdapter](message: => String, cause: Option[C]): URIO[R, Unit] =
      apply(UIO(message), cause)

    /**
     * Submits this event builder with the specified message.
     *
     * @param message An effect that returns the log message.
     * @return The result of submitting this event builder with the specified message.
     */
    def apply(message: UIO[String]): URIO[R, Unit] =
      loggerApi.submit(level, markers, keyValuePairs, message, None)

    /**
     * Submits this event builder with the specified message and cause.
     *
     * @tparam C The type of cause that inspired this log entry.
     * @param message An effect that returns the log message.
     * @param cause   The cause to record with the logging event.
     * @return The result of submitting this event builder with the specified message and cause.
     */
    def apply[C: ThrowableAdapter](message: UIO[String], cause: C): URIO[R, Unit] =
      loggerApi.submit(level, markers, keyValuePairs, message, Some(ThrowableAdapter(cause)))

    /**
     * Submits this event builder with the specified messag and optional cause.
     *
     * @tparam C The type of cause that inspired this log entry.
     * @param message An effect that returns the log message.
     * @param cause   The optional cause to record with the logging event.
     * @return The result of submitting this event builder with the specified message and optional cause.
     */
    def apply[C: ThrowableAdapter](message: UIO[String], cause: Option[C]): URIO[R, Unit] =
      loggerApi.submit(level, markers, keyValuePairs, message, cause map ThrowableAdapter[C]())

  }

  /**
   * Definitions that support event builders.
   */
  object EventBuilder {

    /**
     * A piece of metadata paired with its adapter.
     *
     * @tparam T The underlying type to apply to an event builder.
     */
    case class Metadata[T: MetadataAdapter](data: T) {

      /**
       * Applies the underlying metadata to the specified event builder.
       *
       * @tparam R The type required by the builder.
       * @param builder The builder to apply the underlying metadata to.
       * @return The specified event builder with the underlying metadata applied to it.
       */
      def apply[R](builder: EventBuilder[R]): EventBuilder[R] = MetadataAdapter(builder, data)

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
      implicit def asMetadata[T: MetadataAdapter](data: T): Metadata[T] = Metadata(data)

    }

    /**
     * A type class that applies an instance of the underlying type to an event builder.
     *
     * @tparam T The underlying type to apply to an event builder.
     */
    trait MetadataAdapter[-T] {

      /**
       * Applies the supplied metadata to the specified event builder.
       *
       * @tparam R The type required by the builder.
       * @param builder The builder to apply the metadata to.
       * @param data    The metadata to apply to the builder.
       * @return The specified event builder with the supplied metadata applied to it.
       */
      def apply[R](builder: EventBuilder[R], data: T): EventBuilder[R]

    }

    /**
     * The supported metadata adapter implementations.
     */
    object MetadataAdapter {

      /** Support for all markers. */
      implicit val markerAdapter: MetadataAdapter[Marker] =
        new MetadataAdapter[Marker] {
          override def apply[R](builder: EventBuilder[R], marker: Marker) =
            builder.copy(markers = builder.markers + marker)
        }

      /** Support for all adaptable key / value pairs. */
      implicit def keyValuePairAdapter[V: ReferenceAdapter]: MetadataAdapter[(String, V)] =
        new MetadataAdapter[(String, V)] {
          override def apply[R](builder: EventBuilder[R], pair: (String, V)) =
            builder.copy(keyValuePairs = builder.keyValuePairs + (pair._1 -> ReferenceAdapter(pair._2)))
        }

      /**
       * Returns the implicit metadata adapter type class for the specified type.
       *
       * @tparam T The type to return the implicit metadata adapter type class for.
       * @return The implicit metadata adapter type class for the specified type.
       */
      def apply[T: MetadataAdapter](): MetadataAdapter[T] =
        implicitly[MetadataAdapter[T]]

      /**
       * Applies the supplied metadata to the specified event builder.
       *
       * @tparam R The type required by the builder.
       * @tparam T The type of data to apply to the builder.
       * @param builder The builder to apply the metadata to.
       * @param data    The metadata to apply to the builder.
       * @return The specified event builder with the supplied metadata applied to it.
       */
      def apply[R, T: MetadataAdapter](builder: EventBuilder[R], data: T): EventBuilder[R] =
        MetadataAdapter[T]().apply(builder, data)

    }

    /**
     * A type class that converts an instance of the underlying type into a reference.
     *
     * @tparam T The underlying type to convert into a reference.
     */
    trait ReferenceAdapter[-T] extends (T => AnyRef)

    /**
     * The supported reference adapter implementations.
     */
    object ReferenceAdapter {

      /** Support for all references. */
      implicit val referenceAdapter: ReferenceAdapter[AnyRef] = identity(_)

      /** Support for all booleans. */
      implicit val booleanAdapter: ReferenceAdapter[Boolean] = java.lang.Boolean.valueOf

      /** Support for all bytes. */
      implicit val byteAdapter: ReferenceAdapter[Byte] = java.lang.Byte.valueOf

      /** Support for all shorts. */
      implicit val shortAdapter: ReferenceAdapter[Short] = java.lang.Short.valueOf

      /** Support for all integers. */
      implicit val integerAdapter: ReferenceAdapter[Int] = java.lang.Integer.valueOf

      /** Support for all longs. */
      implicit val longAdapter: ReferenceAdapter[Long] = java.lang.Long.valueOf

      /** Support for all floats. */
      implicit val floatAdapter: ReferenceAdapter[Float] = java.lang.Float.valueOf

      /** Support for all doubles. */
      implicit val doubleAdapter: ReferenceAdapter[Double] = java.lang.Double.valueOf

      /** Support for all characters. */
      implicit val charAdapter: ReferenceAdapter[Char] = java.lang.Character.valueOf

      /**
       * Returns the implicit reference adapter type class for the specified type.
       *
       * @tparam T The type to return the implicit reference adapter type class for.
       * @return The implicit reference adapter type class for the specified type.
       */
      def apply[T: ReferenceAdapter](): ReferenceAdapter[T] = implicitly[ReferenceAdapter[T]]

      /**
       * Converts the specified value into a reference using the implicit reference adapter type class.
       *
       * @tparam T The type of value to convert into a reference using the implicit reference adapter type class.
       * @param value The value to convert into a reference using the implicit reference adapter type class.
       * @return The specified value converted into a reference using the implicit reference adapter type class.
       */
      def apply[T: ReferenceAdapter](value: T): AnyRef = ReferenceAdapter[T]().apply(value)

    }

    /**
     * A type class converts an instance of the underlying type into a throwable.
     *
     * @tparam T The underlying type to convert into a throwable.
     */
    trait ThrowableAdapter[-T] extends (T => Throwable)

    /**
     * The supported throwable adapter implementations.
     */
    object ThrowableAdapter {

      /** Support for all throwables. */
      implicit val throwableAdapter: ThrowableAdapter[Throwable] = identity(_)

      /** Support for all throwable causes. */
      implicit val throwableCauseAdapter: ThrowableAdapter[Cause[Throwable]] = _.squash

      /**
       * Returns the implicit throwable adapter type class for the specified type.
       *
       * @tparam T The type to return the implicit throwable adapter type class for.
       * @return The implicit throwable adapter type class for the specified type.
       */
      def apply[T: ThrowableAdapter](): ThrowableAdapter[T] = implicitly[ThrowableAdapter[T]]

      /**
       * Converts the specified cause into a throwable using the implicit throwable adapter type class.
       *
       * @tparam T The type of cause to convert into a throwable using the implicit throwable adapter type class.
       * @param cause The cause to convert into a throwable using the implicit throwable adapter type class.
       * @return The specified cause converted into a throwable using the implicit throwable adapter type class.
       */
      def apply[T: ThrowableAdapter](cause: T): Throwable = ThrowableAdapter[T]().apply(cause)

    }

  }

}
