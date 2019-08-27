/*
 * Recover.scala
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

import language.implicitConversions

import zio.{Cause, UIO, URIO, ZIO}
import zio.console.Console

/**
 * A low-level, optionally prefixed recovery utility that prints to the console.
 *
 * @param prefix The optional string to prefix console lines with.
 */
private case class Recover(prefix: Option[String] = None) {

  /**
   * Prints the specified reports on the console.
   *
   * @param reports The reports to print on the console.
   * @return An effect that prints the specified reports on the console.
   */
  def apply(reports: Recover.Report[_]*): URIO[Console, Unit] = for {
    report <- UIO {
      val result = new StringWriter()
      val out = new PrintWriter(result)
      val lines = reports.iterator flatMap (_.lines)
      prefix map (p => lines map (l => s"$p $l")) getOrElse lines foreach out.println
      out.flush()
      result.toString
    }
    _ <- if (report.isEmpty) UIO.unit else ZIO.accessM[Console](_.console.putStr(report))
  } yield ()

}

/**
 * A low-level utility that logs various information to the console when loggers cannot (i.e. when a logger fails).
 */
private object Recover {

  /** A regex that matches empty or all-whitespace strings. */
  private val emptyOrWhitespace = "^\\s*$".r

  /**
   * Creates a prefixed recovery utility that prints to the console.
   *
   * @param prefix The string to prefix console lines with.
   */
  def apply(prefix: String): Recover = Recover(Some(prefix))

  /**
   * A console report used when recovering from problems.
   *
   * @tparam I The type of input to report during recovery.
   * @param indent The depth to indent this report.
   * @param input  The input to report during recovery.
   */
  case class Report[I: Input](indent: Int, input: I) {

    /** Returns the non-empty lines in this report, properly indented. */
    def lines: Iterator[String] = {
      val indented = " " * indent
      implicitly[Input[I]]
        .text(input)
        .linesIterator
        .filterNot(emptyOrWhitespace.pattern.matcher(_).matches())
        .map(indented + _)
    }

  }

  /**
   * Implicit support for the report syntax.
   */
  object Report {

    /** Treat any input as an unindented report. */
    implicit def inputToReport[T: Input](input: T): Report[T] = Report(0, input)

    /** Treat any integer and input pair as a properly indented report. */
    implicit def indentedInputToReport[T: Input](input: (Int, T)): Report[T] = Report(input._1, input._2)

  }

  /**
   * Base type for strategies that convert input data into reportable text.
   *
   * @tparam T The type of input data that can be converted into reportable text.
   */
  trait Input[-T] {

    /**
     * Converts the specified input data into reportable text.
     *
     * @param input The input data to convert into reportable text.
     * @return The specified input data converted into reportable text.
     */
    def text(input: T): String

  }

  /**
   * Implicit support for common input types.
   */
  object Input {

    /** Implicit support for string inputs as themselves. */
    implicit val strings: Input[String] = identity(_)

    /** Implicit support for throwable inputs as their stack trace. */
    implicit val throwables: Input[Throwable] = t => {
      val result = new StringWriter()
      val out = new PrintWriter(result)
      t.printStackTrace(out)
      out.flush()
      result.toString
    }

    /** Implicit support for cause inputs as their pretty printed selves. */
    implicit def causes[E]: Input[Cause[E]] = _.prettyPrint

    /** Implicit support for all optional inputs. */
    implicit def options[T: Input]: Input[Option[T]] = _ map implicitly[Input[T]].text getOrElse ""

  }

}
