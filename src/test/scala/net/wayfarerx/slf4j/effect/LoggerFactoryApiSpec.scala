/*
 * LoggerFactoryApiSpec.scala
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

import zio.{Cause, DefaultRuntime, UIO}

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for the logger factory API operations.
 */
final class LoggerFactoryApiSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockLogger = mock[Logger]

  private val mockLoggerFactory = mock[LoggerFactoryApi.Service[Any]]

  private val runtime = new DefaultRuntime {}

  "LoggerFactoryApi" should "return loggers by class name" in {
    (mockLoggerFactory.apply(_: String)).expects(getClass.getName).once().returns(UIO(mockLogger))
    runtime.unsafeRun(mockLoggerFactory(getClass)) shouldBe mockLogger
  }

}
