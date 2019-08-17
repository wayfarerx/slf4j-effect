/*
 * LoggerApiSpec.scala
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
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}
import org.slf4j
import zio.console.Console
import zio.{DefaultRuntime, Task}

/**
 * Test suite for the logger API operations.
 */
final class LoggerApiSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockLogger = mock[slf4j.Logger]

  private val runtime = new DefaultRuntime {} map (_ => Logger.Live(mockLogger, console = mock[Console.Service[Any]]))

  private val thrown = new RuntimeException

  "LoggerAPI" should "support logging at the TRACE level" in {
    (() => mockLogger.isTraceEnabled).expects().returning(true).anyNumberOfTimes()
    (mockLogger.trace(_: String)).expects("message1").returning(()).once()
    (mockLogger.trace(_: String, _: Throwable)).expects("message2", thrown).returning(()).once()
    runtime.unsafeRun {
      for {
        _ <- logger.isTraceEnabled flatMap (e => Task(e shouldBe true))
        _ <- logger.trace("message1")
        _ <- logger.trace("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the TRACE level" in {
    (() => mockLogger.isTraceEnabled).expects().returning(false).anyNumberOfTimes()
    runtime.unsafeRun {
      for {
        _ <- logger.isTraceEnabled flatMap (e => Task(e shouldBe false))
        _ <- logger.trace("message1")
        _ <- logger.trace("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the DEBUG level" in {
    (() => mockLogger.isDebugEnabled).expects().returning(true).anyNumberOfTimes()
    (mockLogger.debug(_: String)).expects("message1").returning(()).once()
    (mockLogger.debug(_: String, _: Throwable)).expects("message2", thrown).returning(()).once()
    runtime.unsafeRun {
      for {
        _ <- logger.isDebugEnabled flatMap (e => Task(e shouldBe true))
        _ <- logger.debug("message1")
        _ <- logger.debug("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the DEBUG level" in {
    (() => mockLogger.isDebugEnabled).expects().returning(false).anyNumberOfTimes()
    runtime.unsafeRun {
      for {
        _ <- logger.isDebugEnabled flatMap (e => Task(e shouldBe false))
        _ <- logger.debug("message1")
        _ <- logger.debug("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the INFO level" in {
    (() => mockLogger.isInfoEnabled).expects().returning(true).anyNumberOfTimes()
    (mockLogger.info(_: String)).expects("message1").returning(()).once()
    (mockLogger.info(_: String, _: Throwable)).expects("message2", thrown).returning(()).once()
    runtime.unsafeRun {
      for {
        _ <- logger.isInfoEnabled flatMap (e => Task(e shouldBe true))
        _ <- logger.info("message1")
        _ <- logger.info("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the INFO level" in {
    (() => mockLogger.isInfoEnabled).expects().returning(false).anyNumberOfTimes()
    runtime.unsafeRun {
      for {
        _ <- logger.isInfoEnabled flatMap (e => Task(e shouldBe false))
        _ <- logger.info("message1")
        _ <- logger.info("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the WARN level" in {
    (() => mockLogger.isWarnEnabled).expects().returning(true).anyNumberOfTimes()
    (mockLogger.warn(_: String)).expects("message1").returning(()).once()
    (mockLogger.warn(_: String, _: Throwable)).expects("message2", thrown).returning(()).once()
    runtime.unsafeRun {
      for {
        _ <- logger.isWarnEnabled flatMap (e => Task(e shouldBe true))
        _ <- logger.warn("message1")
        _ <- logger.warn("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the WARN level" in {
    (() => mockLogger.isWarnEnabled).expects().returning(false).anyNumberOfTimes()
    runtime.unsafeRun {
      for {
        _ <- logger.isWarnEnabled flatMap (e => Task(e shouldBe false))
        _ <- logger.warn("message1")
        _ <- logger.warn("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the ERROR level" in {
    (() => mockLogger.isErrorEnabled).expects().returning(true).anyNumberOfTimes()
    (mockLogger.error(_: String)).expects("message1").returning(()).once()
    (mockLogger.error(_: String, _: Throwable)).expects("message2", thrown).returning(()).once()
    runtime.unsafeRun {
      for {
        _ <- logger.isErrorEnabled flatMap (e => Task(e shouldBe true))
        _ <- logger.error("message1")
        _ <- logger.error("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the ERROR level" in {
    (() => mockLogger.isErrorEnabled).expects().returning(false).anyNumberOfTimes()
    runtime.unsafeRun {
      for {
        _ <- logger.isErrorEnabled flatMap (e => Task(e shouldBe false))
        _ <- logger.error("message1")
        _ <- logger.error("message2", thrown)
      } yield ()
    }
  }

}
