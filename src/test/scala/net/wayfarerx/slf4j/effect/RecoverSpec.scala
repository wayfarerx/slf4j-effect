/*
 * RecoverSpec.scala
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

import java.io.{PrintWriter, StringWriter}
import java.time.OffsetDateTime

import zio.console.Console
import zio.{Cause, DefaultRuntime, UIO}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}
import zio.clock.Clock

/**
 * Test suite for the recovery operation.
 */
final class RecoverSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val runtime = new DefaultRuntime {}

  private val mockClockService = mock[Clock.Service[Any]]
  private val mockConsoleService = mock[Console.Service[Any]]

  private val mockEnvironment = new Clock with Console {
    override val clock: Clock.Service[Any] = mockClockService
    override val console: Console.Service[Any] = mockConsoleService
  }

  private val prefix = "PREFIX"
  private val now = OffsetDateTime.now

  "Recover" should "log string reports to the console" in {
    (mockConsoleService.putStr _).expects(printed(0 -> "failed")).returns(UIO.unit).once()
    runtime.unsafeRun(Recover(prefix, Some(now))("failed").provide(mockEnvironment))
    runtime.unsafeRun(Recover(prefix, Some(now))(None: Option[String]).provide(mockEnvironment))
  }

  it should "log throwable reports to the console" in {
    val thrown = new RuntimeException
    val expected = new StringWriter()
    val out = new PrintWriter(expected)
    thrown.printStackTrace(out)
    out.flush()
    (mockConsoleService.putStr _).expects(printed(0 -> expected.toString)).returns(UIO.unit).once()
    runtime.unsafeRun(Recover(prefix, Some(now))(thrown).provide(mockEnvironment))
    runtime.unsafeRun(Recover(prefix, Some(now))(None: Option[Throwable]).provide(mockEnvironment))
  }

  it should "log cause reports to the console" in {
    val cause = Cause.fail(new RuntimeException)
    (mockConsoleService.putStr _).expects(printed(0 -> cause.prettyPrint)).returns(UIO.unit).once()
    runtime.unsafeRun(Recover(prefix, Some(now))(cause).provide(mockEnvironment))
    runtime.unsafeRun(Recover(prefix, Some(now))(None: Option[Cause[Any]]).provide(mockEnvironment))
  }

  it should "log optional reports to the console" in {
    (mockConsoleService.putStr _).expects(printed(0 -> "failed")).returns(UIO.unit).once()
    runtime.unsafeRun(Recover(prefix, Some(now))(Some("failed")).provide(mockEnvironment))
    runtime.unsafeRun(Recover(prefix, Some(now))(None: Option[String]).provide(mockEnvironment))
  }

  it should "log indented reports to the console" in {
    (mockConsoleService.putStr _).expects(printed(2 -> "failed")).returns(UIO.unit).once()
    runtime.unsafeRun(Recover(prefix, Some(now))(2 -> "failed").provide(mockEnvironment))
    runtime.unsafeRun(Recover(prefix, Some(now))(2 -> (None: Option[String])).provide(mockEnvironment))
  }

  /** Normalize the specified sequence of chunks. */
  private def printed(chunks: (Int, String)*): String = {
    val result = new StringWriter()
    val out = new PrintWriter(result)
    chunks.iterator flatMap { case (indent, chunk) =>
      chunk.linesIterator filterNot (_.trim.isEmpty) map { line =>
        val result = s"$now${" " * indent} $line"
        if (prefix.isEmpty) result else s"$prefix $result"
      }
    } foreach out.println
    out.flush()
    result.toString
  }

}
