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

import org.slf4j

import zio.{Cause, DefaultRuntime, UIO}

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for the logger API operations.
 */
final class OldLoggerApiSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockLogger = mock[OldLoggerApi.Service[Any]]

  private val mockSlf4jMarker = mock[slf4j.Marker]

  private val runtime = new DefaultRuntime {}

  private val thrown = new RuntimeException

  "LoggerApi" should "filter and log TRACE events" in {
    (mockLogger.isEnabled(_: Level)).expects(Level.Trace).once().returns(UIO(true))
    runtime.unsafeRun(mockLogger.isTraceEnabled) shouldBe true
    mockLogger.trace shouldBe OldLoggerApi.EventBuilder[Any](mockLogger, Level.Trace)
  }

  it should "filter and log DEBUG events" in {
    (mockLogger.isEnabled(_: Level)).expects(Level.Debug).once().returns(UIO(true))
    runtime.unsafeRun(mockLogger.isDebugEnabled) shouldBe true
    mockLogger.debug shouldBe OldLoggerApi.EventBuilder[Any](mockLogger, Level.Debug)
  }

  it should "filter and log INFO events" in {
    (mockLogger.isEnabled(_: Level)).expects(Level.Info).once().returns(UIO(true))
    runtime.unsafeRun(mockLogger.isInfoEnabled) shouldBe true
    mockLogger.info shouldBe OldLoggerApi.EventBuilder[Any](mockLogger, Level.Info)
  }

  it should "filter and log WARN events" in {
    (mockLogger.isEnabled(_: Level)).expects(Level.Warn).once().returns(UIO(true))
    runtime.unsafeRun(mockLogger.isWarnEnabled) shouldBe true
    mockLogger.warn shouldBe OldLoggerApi.EventBuilder[Any](mockLogger, Level.Warn)
  }

  it should "filter and log ERROR events" in {
    (mockLogger.isEnabled(_: Level)).expects(Level.Error).once().returns(UIO(true))
    runtime.unsafeRun(mockLogger.isErrorEnabled) shouldBe true
    mockLogger.error shouldBe OldLoggerApi.EventBuilder[Any](mockLogger, Level.Error)
  }

  "LoggerApi.EventBuilder" should "support event metadata" in {
    val marker = new Marker(mockSlf4jMarker)
    mockLogger.trace(
      "boolean" -> true,
      "byte" -> 1.toByte,
      "short" -> 2.toShort,
      "int" -> 3,
      "long" -> 4L,
      marker,
      "float" -> 5f,
      "double" -> 6.0,
      "char" -> 'x',
      "string" -> "str"
    ) shouldBe OldLoggerApi.EventBuilder[Any](mockLogger, Level.Trace, markers = Set(marker), keyValuePairs = Map(
      "boolean" -> java.lang.Boolean.TRUE,
      "byte" -> java.lang.Byte.valueOf(1.toByte),
      "short" -> java.lang.Short.valueOf(2.toShort),
      "int" -> java.lang.Integer.valueOf(3),
      "long" -> java.lang.Long.valueOf(4L),
      "float" -> java.lang.Float.valueOf(5f),
      "double" -> java.lang.Double.valueOf(6.0),
      "char" -> java.lang.Character.valueOf('x'),
      "string" -> "str"
    ))
  }

  it should "submit logging messages" in {
    (mockLogger.submit _).expects(Level.Debug, Set[Marker](), Map[String, AnyRef](), *, None).once()
      .returns(UIO.unit)
    mockLogger.debug("msg") shouldBe UIO.unit
  }

  it should "submit logging messages with throwables" in {
    (mockLogger.submit _).expects(Level.Info, Set[Marker](), Map("a" -> ("b": AnyRef)), *, Some(thrown)).once()
      .returns(UIO.unit)
    mockLogger.info("a" -> "b")("msg", thrown) shouldBe UIO.unit
  }

  it should "submit logging messages with causes" in {
    (mockLogger.submit _).expects(Level.Warn, Set[Marker](), Map[String, AnyRef](), *, Some(thrown)).once()
      .returns(UIO.unit)
    mockLogger.warn("msg", Some(Cause.fail(thrown))) shouldBe UIO.unit
  }

}
