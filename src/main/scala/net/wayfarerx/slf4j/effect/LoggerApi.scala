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

import language.higherKinds

import zio.URIO

/**
 * Definition of the API for accessing SLF4J loggers.
 *
 * @tparam R The environment type to require in effects.
 */
trait LoggerApi[-R] {

  /** The type of effect returned by this API. */
  type Effect[+A] <: URIO[R, A]

  /**
   * Returns true if the `TRACE` logging level is enabled.
   *
   * @return An effect that returns true if the `TRACE` logging level is enabled.
   */
  final def isTraceEnabled: Effect[Boolean] = isEnabled(Level.Trace)

  /**
   * Logs a message at the `TRACE` logging level.
   *
   * @param f A function that returns the log message.
   * @return An effect that logs a message at the `TRACE` logging level.
   */
  final def trace(f: => String): Effect[Unit] = log(Level.Trace, f)

  /**
   * Logs a message and `Throwable` at the `TRACE` logging level.
   *
   * @param f A function that returns the log message.
   * @param t The `Throwable` to log.
   * @return An effect that logs a message and `Throwable` at the `TRACE` logging level.
   */
  final def trace(f: => String, t: Throwable): Effect[Unit] = log(Level.Trace, f, t)

  /**
   * Returns true if the `DEBUG` logging level is enabled.
   *
   * @return An effect that returns true if the `DEBUG` logging level is enabled.
   */
  final def isDebugEnabled: Effect[Boolean] = isEnabled(Level.Debug)

  /**
   * Logs a message at the `DEBUG` logging level.
   *
   * @param f A function that returns the log message.
   * @return An effect that logs a message at the `DEBUG` logging level.
   */
  final def debug(f: => String): Effect[Unit] = log(Level.Debug, f)

  /**
   * Logs a message and `Throwable` at the `DEBUG` logging level.
   *
   * @param f A function that returns the log message.
   * @param t The `Throwable` to log.
   * @return An effect that logs a message and `Throwable` at the `DEBUG` logging level.
   */
  final def debug(f: => String, t: Throwable): Effect[Unit] = log(Level.Debug, f, t)

  /**
   * Returns true if the `INFO` logging level is enabled.
   *
   * @return An effect that returns true if the `INFO` logging level is enabled.
   */
  final def isInfoEnabled: Effect[Boolean] = isEnabled(Level.Info)

  /**
   * Logs a message at the `INFO` logging level.
   *
   * @param f A function that returns the log message.
   * @return An effect that logs a message at the `INFO` logging level.
   */
  final def info(f: => String): Effect[Unit] = log(Level.Info, f)

  /**
   * Logs a message and `Throwable` at the `INFO` logging level.
   *
   * @param f A function that returns the log message.
   * @param t The `Throwable` to log.
   * @return An effect that logs a message and `Throwable` at the `INFO` logging level.
   */
  final def info(f: => String, t: Throwable): Effect[Unit] = log(Level.Info, f, t)

  /**
   * Returns true if the `WARN` logging level is enabled.
   *
   * @return An effect that returns true if the `WARN` logging level is enabled.
   */
  final def isWarnEnabled: Effect[Boolean] = isEnabled(Level.Warn)

  /**
   * Logs a message at the `WARN` logging level.
   *
   * @param f A function that returns the log message.
   * @return An effect that logs a message at the `WARN` logging level.
   */
  final def warn(f: => String): Effect[Unit] = log(Level.Warn, f)

  /**
   * Logs a message and `Throwable` at the `WARN` logging level.
   *
   * @param f A function that returns the log message.
   * @param t The `Throwable` to log.
   * @return An effect that logs a message and `Throwable` at the `WARN` logging level.
   */
  final def warn(f: => String, t: Throwable): Effect[Unit] = log(Level.Warn, f, t)

  /**
   * Returns true if the `ERROR` logging level is enabled.
   *
   * @return An effect that returns true if the `ERROR` logging level is enabled.
   */
  final def isErrorEnabled: Effect[Boolean] = isEnabled(Level.Error)

  /**
   * Logs a message at the `ERROR` logging level.
   *
   * @param f A function that returns the log message.
   * @return An effect that logs a message at the `ERROR` logging level.
   */
  final def error(f: => String): Effect[Unit] = log(Level.Error, f)

  /**
   * Logs a message and `Throwable` at the `ERROR` logging level.
   *
   * @param f A function that returns the log message.
   * @param t The `Throwable` to log.
   * @return An effect that logs a message and `Throwable` at the `ERROR` logging level.
   */
  final def error(f: => String, t: Throwable): Effect[Unit] = log(Level.Error, f, t)

  /**
   * Returns true if the specified logging level is enabled.
   *
   * @param level The logging level to check the status of.
   * @return An effect that returns true if the specified logging level is enabled.
   */
  def isEnabled(level: Level): Effect[Boolean]

  /**
   * Logs a message at the specified level.
   *
   * @param level The level to log the message at.
   * @param f     A function that returns the log message.
   * @return An effect that logs a message at the specified level.
   */
  def log(level: Level, f: => String): Effect[Unit]

  /**
   * Logs a message and `Throwable` at the specified level.
   *
   * @param level The level to log the message and `Throwable` at.
   * @param f     A function that returns the log message.
   * @param t     The `Throwable` to log.
   * @return An effect that logs a message and `Throwable` at the specified level.
   */
  def log(level: Level, f: => String, t: Throwable): Effect[Unit]

}

/**
 * Definitions that support the `LoggerApi` trait.
 */
object LoggerApi {

  /** The type of logger APIs with a fixed environment. */
  type Aux[R] = LoggerApi[R] {type Effect[+A] = URIO[R, A]}

}