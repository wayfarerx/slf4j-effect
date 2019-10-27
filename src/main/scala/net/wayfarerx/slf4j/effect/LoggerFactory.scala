/*
 * LoggerFactory.scala
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

import zio.{Task, ZIO}
import zio.blocking.Blocking
import zio.console.Console

/**
 * Environment mix-in that exposes the logger factory API.
 */
trait LoggerFactory {

  /** The exposed logger factory API. */
  val loggerFactory: LoggerFactoryApi[Any]

}

/**
 * The global logger factory service and definitions that support the `LoggerFactory` environment.
 */
object LoggerFactory extends (LoggerFactoryApi[Any] => LoggerFactory) with LoggerFactoryApi.Service[LoggerFactory] {

  /**
   * Creates a new logger factory from the specified logger factory service.
   *
   * @param loggerFactory The logger factory service to use in the new logger factory.
   * @return A new logger factory from the specified logger factory service.
   */
  override def apply(loggerFactory: LoggerFactoryApi[Any]): LoggerFactory = {
    val _loggerFactory = loggerFactory
    new LoggerFactory {
      override val loggerFactory = _loggerFactory
    }
  }

  /* Return a logger named according to the name parameter. */
  override def apply(name: String): Result[LoggerOld] =
    ZIO.accessM(_.loggerFactory(name))

  /**
   * Implementation of the `LoggerFactory` environment using a SLF4J `ILoggerFactory`.
   */
  trait Live extends LoggerFactory {
    self: Blocking with Console =>

    /** The underlying SLF4J `ILoggerFactory`. */
    val slf4jLoggerFactory: slf4j.ILoggerFactory

    /* Implement the logger factory API. */
    final override val loggerFactory: LoggerFactoryApi[Any] = new LoggerFactoryApi.Service[Any] {

      /* Return a logger named according to the name parameter. */
      override def apply(name: String): Result[LoggerOld] =
        Task(slf4jLoggerFactory.getLogger(name)) map (LoggerOld.Live(_, self.blocking, self.console))

    }

  }

  /**
   * Factory for live `LoggerFactory` implementations.
   */
  object Live extends ((
    slf4j.ILoggerFactory,
      Blocking.Service[Any],
      Console.Service[Any]
    ) => Live with Blocking with Console) {

    /**
     * Creates a live `LoggerFactory`.
     *
     * @return A live `LoggerFactory`.
     */
    def apply(): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getILoggerFactory) map (Live(_))

    /**
     * Creates a live `LoggerFactory` with the specified blocking service.
     *
     * @param blocking The blocking service to use.
     * @return A live `LoggerFactory` with the specified blocking service.
     */
    def apply(blocking: Blocking.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getILoggerFactory) map (Live(_, blocking))

    /**
     * Creates a live `LoggerFactory` with the specified console service.
     *
     * @param console The console service to use.
     * @return A live `LoggerFactory` with the specified console service.
     */
    def apply(console: Console.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getILoggerFactory) map (Live(_, console = console))

    /**
     * Creates a live `LoggerFactory` with the specified blocking and console service.
     *
     * @param blocking The blocking service to use.
     * @param console The console service to use.
     * @return A live `LoggerFactory` with the specified blocking and console service.
     */
    def apply(blocking: Blocking.Service[Any], console: Console.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.LoggerFactory.getILoggerFactory) map (Live(_, blocking, console))

    /**
     * Creates a live `LoggerFactory` implementation with the specified blocking and console service.
     *
     * @param slf4jLoggerFactory The SLF4J logger factory to use.
     * @param blocking The blocking service to use, defaults to the global blocking service.
     * @param console The console service to use, defaults to the global console service.
     * @return A live `LoggerFactory` implementation with the specified blocking and console service.
     */
    override def apply(
      slf4jLoggerFactory: slf4j.ILoggerFactory,
      blocking: Blocking.Service[Any] = Blocking.Live.blocking,
      console: Console.Service[Any] = Console.Live.console
    ): Live with Blocking with Console = {
      val _slf4jLoggerFactory = slf4jLoggerFactory
      val _blocking = blocking
      val _console = console
      new Live with Blocking with Console {
        override val slf4jLoggerFactory = _slf4jLoggerFactory
        override val blocking = _blocking
        override val console = _console
      }
    }

  }

}
