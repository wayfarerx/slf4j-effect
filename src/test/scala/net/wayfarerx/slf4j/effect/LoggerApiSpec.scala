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

  import net.wayfarerx.slf4j.{effect => pkg}

  private val mockLogger = mock[slf4j.Logger]

  private val runtime = new DefaultRuntime {} map (_ => Logger.Live(mockLogger, console = mock[Console.Service[Any]]))

  private val thrown = new RuntimeException

  "LoggerAPI" should "support logging at the TRACE level" in {
    (() => mockLogger.isTraceEnabled).expects().returning(true).anyNumberOfTimes
    (mockLogger.trace(_: String)).expects("message1").returning(()).once
    (mockLogger.trace(_: String, _: Throwable)).expects("message2", thrown).returning(()).once
    runtime.unsafeRun {
      for {
        _ <- isTraceEnabled flatMap (e => Task(e shouldBe true))
        _ <- trace("message1")
        _ <- trace("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the TRACE level" in {
    (() => mockLogger.isTraceEnabled).expects().returning(false).anyNumberOfTimes
    runtime.unsafeRun {
      for {
        _ <- isTraceEnabled flatMap (e => Task(e shouldBe false))
        _ <- trace("message1")
        _ <- trace("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the DEBUG level" in {
    (() => mockLogger.isDebugEnabled).expects().returning(true).anyNumberOfTimes
    (mockLogger.debug(_: String)).expects("message1").returning(()).once
    (mockLogger.debug(_: String, _: Throwable)).expects("message2", thrown).returning(()).once
    runtime.unsafeRun {
      for {
        _ <- isDebugEnabled flatMap (e => Task(e shouldBe true))
        _ <- debug("message1")
        _ <- debug("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the DEBUG level" in {
    (() => mockLogger.isDebugEnabled).expects().returning(false).anyNumberOfTimes
    runtime.unsafeRun {
      for {
        _ <- isDebugEnabled flatMap (e => Task(e shouldBe false))
        _ <- debug("message1")
        _ <- debug("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the INFO level" in {
    (() => mockLogger.isInfoEnabled).expects().returning(true).anyNumberOfTimes
    (mockLogger.info(_: String)).expects("message1").returning(()).once
    (mockLogger.info(_: String, _: Throwable)).expects("message2", thrown).returning(()).once
    runtime.unsafeRun {
      for {
        _ <- isInfoEnabled flatMap (e => Task(e shouldBe true))
        _ <- pkg.info("message1")
        _ <- pkg.info("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the INFO level" in {
    (() => mockLogger.isInfoEnabled).expects().returning(false).anyNumberOfTimes
    runtime.unsafeRun {
      for {
        _ <- isInfoEnabled flatMap (e => Task(e shouldBe false))
        _ <- pkg.info("message1")
        _ <- pkg.info("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the WARN level" in {
    (() => mockLogger.isWarnEnabled).expects().returning(true).anyNumberOfTimes
    (mockLogger.warn(_: String)).expects("message1").returning(()).once
    (mockLogger.warn(_: String, _: Throwable)).expects("message2", thrown).returning(()).once
    runtime.unsafeRun {
      for {
        _ <- isWarnEnabled flatMap (e => Task(e shouldBe true))
        _ <- warn("message1")
        _ <- warn("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the WARN level" in {
    (() => mockLogger.isWarnEnabled).expects().returning(false).anyNumberOfTimes
    runtime.unsafeRun {
      for {
        _ <- isWarnEnabled flatMap (e => Task(e shouldBe false))
        _ <- warn("message1")
        _ <- warn("message2", thrown)
      } yield ()
    }
  }

  it should "support logging at the ERROR level" in {
    (() => mockLogger.isErrorEnabled).expects().returning(true).anyNumberOfTimes
    (mockLogger.error(_: String)).expects("message1").returning(()).once
    (mockLogger.error(_: String, _: Throwable)).expects("message2", thrown).returning(()).once
    runtime.unsafeRun {
      for {
        _ <- isErrorEnabled flatMap (e => Task(e shouldBe true))
        _ <- error("message1")
        _ <- error("message2", thrown)
      } yield ()
    }
  }

  it should "support NOT logging at the ERROR level" in {
    (() => mockLogger.isErrorEnabled).expects().returning(false).anyNumberOfTimes
    runtime.unsafeRun {
      for {
        _ <- isErrorEnabled flatMap (e => Task(e shouldBe false))
        _ <- error("message1")
        _ <- error("message2", thrown)
      } yield ()
    }
  }

}