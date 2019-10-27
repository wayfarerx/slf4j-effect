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

import java.util.function.Supplier

import org.slf4j.{Logger => Slf4jLogger, Marker => Slf4jMarker, MDC => Slf4jMDC, spi => slf4j}

import zio.{DefaultRuntime, UIO}
import zio.blocking.Blocking
import zio.console.Console

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for loggers.
 */
final class LoggerOldSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockSlf4jLogger = mock[Slf4jLogger]

  private val mockSlf4jLoggingEventBuilder = mock[slf4j.LoggingEventBuilder]

  private val mockSlf4jMarker = mock[Slf4jMarker]

  private val mockConsole = mock[Console.Service[Any]]

  private val runtime = new DefaultRuntime {}

  private val thrown = new RuntimeException

  "Logger" should "log trace messages" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    (() => mockSlf4jLogger.isTraceEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atTrace()).expects().returning(mockSlf4jLoggingEventBuilder).once()
    (mockSlf4jLoggingEventBuilder.log(_: String)).expects("msg").returns(()).once()
    runtime.unsafeRun(LoggerOld.isTraceEnabled.provide(logger)) shouldBe true
    runtime.unsafeRun(LoggerOld.trace("msg").provide(logger)).shouldBe(())
  }

  it should "log debug messages" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    (() => mockSlf4jLogger.isDebugEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atDebug()).expects().returning(mockSlf4jLoggingEventBuilder).once()
    (mockSlf4jLoggingEventBuilder.setCause _).expects(thrown).returns(mockSlf4jLoggingEventBuilder).once()
    (mockSlf4jLoggingEventBuilder.log(_: String)).expects("msg").returns(()).once()
    runtime.unsafeRun(LoggerOld.isDebugEnabled.provide(logger)) shouldBe true
    runtime.unsafeRun(LoggerOld.debug("msg", thrown).provide(logger)).shouldBe(())
  }

  it should "log info messages" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    (() => mockSlf4jLogger.isInfoEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atInfo()).expects().returning(mockSlf4jLoggingEventBuilder).once()
    (mockSlf4jLoggingEventBuilder.log(_: String)).expects("msg").returns(()).once()
    runtime.unsafeRun(LoggerOld.isInfoEnabled.provide(logger)) shouldBe true
    runtime.unsafeRun(LoggerOld.info("msg").provide(logger)).shouldBe(())
  }

  it should "log warn messages" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    (() => mockSlf4jLogger.isWarnEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atWarn()).expects().returning(mockSlf4jLoggingEventBuilder).once()
    (mockSlf4jLoggingEventBuilder.log(_: String)).expects("msg").returns(()).once()
    runtime.unsafeRun(LoggerOld.isWarnEnabled.provide(logger)) shouldBe true
    runtime.unsafeRun(LoggerOld.warn("msg").provide(logger)).shouldBe(())
  }

  it should "log error messages" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    (() => mockSlf4jLogger.isErrorEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atError()).expects().returning(mockSlf4jLoggingEventBuilder).once()
    (mockSlf4jLoggingEventBuilder.log(_: String)).expects("msg").returns(()).once()
    runtime.unsafeRun(LoggerOld.isErrorEnabled.provide(logger)) shouldBe true
    runtime.unsafeRun(LoggerOld.error("msg").provide(logger)).shouldBe(())
  }

  it should "filter out disabled levels" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    (() => mockSlf4jLogger.isErrorEnabled).expects().returning(false).anyNumberOfTimes()
    runtime.unsafeRun(LoggerOld.isErrorEnabled.provide(logger)) shouldBe false
    runtime.unsafeRun(LoggerOld.error("msg").provide(logger)).shouldBe(())
  }

  /*
  it should "propagate markers" in {
    val logger = Logger(Logger.Live(mockSlf4jLogger).logger)
    val mockLoggingEventBuilder = new MockLoggingEventBuilder
    (() => mockSlf4jLogger.isErrorEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atError()).expects().returning(mockLoggingEventBuilder).once()
    runtime.unsafeRun(Logger.error(new Marker(mockSlf4jMarker))("msg").provide(logger)).shouldBe(())
    mockLoggingEventBuilder.marker shouldBe Some(mockSlf4jMarker)
    mockLoggingEventBuilder.logged shouldBe Some("msg")
  }
  */

  it should "propagate key/value pairs" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    val mockLoggingEventBuilder = new MockLoggingEventBuilder
    (() => mockSlf4jLogger.isErrorEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atError()).expects().returning(mockLoggingEventBuilder).once()
    runtime.unsafeRun(LoggerOld.error("key" -> "value")("msg").provide(logger)).shouldBe(())
    mockLoggingEventBuilder.keyValue shouldBe Some("key" -> "value")
    mockLoggingEventBuilder.logged shouldBe Some("msg")
  }

  it should "propagate mapped diagnostic contexts" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger).logger)
    val mockLoggingEventBuilder = new MockLoggingEventBuilder
    (() => mockSlf4jLogger.isErrorEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atError()).expects().returning(mockLoggingEventBuilder).once()
    runtime.unsafeRun(MDC("key" -> "value")(LoggerOld.error("msg")).provide(logger)).shouldBe(())
    mockLoggingEventBuilder.mdc shouldBe Some(Map("key" -> "value"))
    mockLoggingEventBuilder.logged shouldBe Some("msg")
  }

  it should "recover from logging failures" in {
    val logger = LoggerOld(LoggerOld.Live(mockSlf4jLogger, console = mockConsole).logger)
    (() => mockSlf4jLogger.isErrorEnabled).expects().returning(true).anyNumberOfTimes()
    (() => mockSlf4jLogger.atError()).expects().returning(mockSlf4jLoggingEventBuilder).once()
    (mockSlf4jLoggingEventBuilder.log(_: String)).expects("msg").throws(thrown).once()
    (mockConsole.putStr _).expects(where[String](_.startsWith(s"${Level.Error} "))).returning(UIO.unit).once()
    runtime.unsafeRun(LoggerOld.error("msg").provide(logger)).shouldBe(())
  }

  it should "construct live instances from logger names" in {
    runtime.unsafeRun(LoggerOld.Live("test")) should not be null
    runtime.unsafeRun(LoggerOld.Live("test", Blocking.Live.blocking)) should not be null
    runtime.unsafeRun(LoggerOld.Live("test", console = Console.Live.console)) should not be null
    runtime.unsafeRun(LoggerOld.Live("test", Blocking.Live.blocking, Console.Live.console)) should not be null
  }

  /**
   * A mock to test setting key/value pairs on SLF4J logging event builders.
   */
  private final class MockLoggingEventBuilder extends slf4j.LoggingEventBuilder {

    var marker: Option[Slf4jMarker] = None

    var keyValue: Option[(String, AnyRef)] = None

    var logged: Option[String] = None

    var mdc: Option[Map[String, String]] = None

    override def setCause(cause: Throwable) = throw thrown

    override def addMarker(m: Slf4jMarker) = {
      marker = Some(m)
      this
    }

    override def addArgument(p: AnyRef) = throw thrown

    override def addArgument(objectSupplier: Supplier[AnyRef]) = throw thrown

    override def addKeyValue(key: String, value: AnyRef) = {
      keyValue = Some(key -> value)
      this
    }

    override def addKeyValue(key: String, value: Supplier[AnyRef]) = throw thrown

    override def log(message: String): Unit = {
      var mdc = Map.empty[String, String]
      Option(Slf4jMDC.getCopyOfContextMap) foreach {
        _.entrySet().iterator().forEachRemaining(e => mdc += e.getKey -> e.getValue)
      }
      if (mdc.nonEmpty) this.mdc = Some(mdc)
      logged = Some(message)
    }

    override def log(message: String, arg: AnyRef): Unit = throw thrown

    override def log(message: String, arg0: AnyRef, arg1: AnyRef): Unit = throw thrown

    override def log(message: String, args: AnyRef*): Unit = throw thrown

    override def log(messageSupplier: Supplier[String]): Unit = throw thrown
  }

}
