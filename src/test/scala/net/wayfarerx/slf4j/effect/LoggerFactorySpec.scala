/*
 * LoggerFactorySpec.scala
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

import org.slf4j.{ILoggerFactory => Slf4jLoggerFactory, Logger => Slf4jLogger}

import zio.DefaultRuntime
import zio.blocking.Blocking
import zio.console.Console

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for logger factories.
 */
final class LoggerFactorySpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockSlf4jLogger = mock[Slf4jLogger]

  private val mockSlf4jLoggerFactory = mock[Slf4jLoggerFactory]

  private val runtime = new DefaultRuntime {}

  "LoggerFactory" should "create named loggers" in {
    val loggerFactory = LoggerFactory(LoggerFactory.Live(mockSlf4jLoggerFactory).loggerFactory)
    (mockSlf4jLoggerFactory.getLogger _).expects("logger").returning(mockSlf4jLogger).once()
    runtime.unsafeRun(LoggerFactory("logger").provide(loggerFactory)) should not be null
  }

  it should "construct live instances from the global logger factory" in {
    runtime.unsafeRun(LoggerFactory.Live()) should not be null
    runtime.unsafeRun(LoggerFactory.Live(Blocking.Live.blocking)) should not be null
    runtime.unsafeRun(LoggerFactory.Live(console = Console.Live.console)) should not be null
    runtime.unsafeRun(LoggerFactory.Live(Blocking.Live.blocking, Console.Live.console)) should not be null
  }

}
