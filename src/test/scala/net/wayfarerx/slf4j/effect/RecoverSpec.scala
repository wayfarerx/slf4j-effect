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

import zio.console.Console
import zio.{Cause, DefaultRuntime, UIO}

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for the recovery operation.
 */
final class RecoverSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val runtime = new DefaultRuntime {}

  private val mockConsoleService = mock[Console.Service[Any]]

  private val mockConsole = new Console {
    override val console: Console.Service[Any] = mockConsoleService
  }

  "Recover" should "log string reports to the console" in {
    (mockConsoleService.putStr _).expects(printed(0 -> "Operation failed")).returns(UIO.unit).once()
    runtime.unsafeRun(Recover()("Operation failed").provide(mockConsole))
  }

  it should "log throwable reports to the console" in {
    val thrown = new RuntimeException
    val expected = new StringWriter()
    val out = new PrintWriter(expected)
    thrown.printStackTrace(out)
    out.flush()
    (mockConsoleService.putStr _).expects(printed(0 -> expected.toString)).returns(UIO.unit).once()
    runtime.unsafeRun(Recover()(thrown).provide(mockConsole))
  }

  it should "log cause reports to the console" in {
    val cause = Cause.fail(new RuntimeException)
    (mockConsoleService.putStr _).expects(printed(0 -> cause.prettyPrint)).returns(UIO.unit).once()
    runtime.unsafeRun(Recover()(cause).provide(mockConsole))
  }

  it should "log optional reports to the console" in {
    (mockConsoleService.putStr _).expects(printed(0 -> "Operation failed")).returns(UIO.unit).once()
    runtime.unsafeRun(Recover()(Some("Operation failed")).provide(mockConsole))
    runtime.unsafeRun(Recover()(None: Option[String]).provide(mockConsole))
  }

  it should "log indented reports to the console" in {
    (mockConsoleService.putStr _).expects(printed(2 -> "Operation failed")).returns(UIO.unit).once()
    runtime.unsafeRun(Recover()(2 -> "Operation failed").provide(mockConsole))
    runtime.unsafeRun(Recover()(None: Option[String]).provide(mockConsole))
  }

  it should "log prefixed reports to the console" in {
    (mockConsoleService.putStr _).expects(printed(0 -> "PREFIX Operation failed")).returns(UIO.unit).once()
    runtime.unsafeRun(Recover("PREFIX")("Operation failed").provide(mockConsole))
  }

  /** Normalize the specified sequence of chunks. */
  private def printed(chunks: (Int, String)*): String = {
    val result = new StringWriter()
    val out = new PrintWriter(result)
    chunks.iterator flatMap { case (indent, chunk) =>
      chunk.linesIterator map (" " * indent + _)
    } filterNot (_.trim.isEmpty) foreach out.println
    out.flush()
    result.toString
  }

}
