/*
 * LoggerSpec.scala
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

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers}

import org.slf4j.{Logger => Slf4jLogger}

import zio.{DefaultRuntime, Task, UIO}
import zio.console.Console

/**
 * Test suite for loggers.
 */
final class LoggerSpec extends FlatSpec with Matchers with MockFactory {

  private val runtime = new DefaultRuntime {}

  "Logger" should "support common factory patterns" in {
    runtime.unsafeRun {
      for {
        _ <- Logger(mock[Slf4jLogger]) flatMap (l => Task(l should not be null))
        _ <- Logger("my-logger") flatMap (l => Task(l should not be null))
        _ <- Logger(classOf[LoggerSpec]) flatMap (l => Task(l should not be null))
        _ <- Logger.connect(mock[Slf4jLogger]) flatMap (l => Task(l should not be null))
        _ <- Logger.connect("my-logger") flatMap (l => Task(l should not be null))
        _ <- Logger.connect(classOf[LoggerSpec]) flatMap (l => Task(l should not be null))
      } yield ()
    }
  }

  it should "recover from logging failures" in {
    val thrown = new RuntimeException
    val mockLogger = mock[Slf4jLogger]
    (() => mockLogger.isErrorEnabled).expects().returning(true).twice()
    (mockLogger.error(_: String)).expects("message1").throws(new RuntimeException).once()
    (mockLogger.error(_: String, _: Throwable)).expects("message2", thrown).throws(new RuntimeException).once()
    val mockConsole = mock[Console.Service[Any]]
    (mockConsole.putStr _).expects(where[String](_.startsWith(s"${Level.Error} "))).returning(UIO.unit).twice()
    val logger = Logger.Live(mockLogger, console = mockConsole).logger
    runtime.unsafeRun {
      for {
        _ <- logger.error("message1")
        _ <- logger.error("message2", thrown)
      } yield ()
    }
  }

  it should "use the default console by default" in {
    Logger.Live(mock[Slf4jLogger]) should not be null
  }

}
