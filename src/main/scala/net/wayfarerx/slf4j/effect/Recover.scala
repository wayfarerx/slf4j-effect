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
import java.time.{LocalDateTime, OffsetDateTime}

import language.implicitConversions

import zio.{Cause, UIO, URIO, ZIO}
import zio.clock.Clock
import zio.console.Console

/**
 * An internal utility that prints various information to the console when loggers fail.
 */
private object Recover {

  /**
   * Prints the specified reports to the console.
   *
   * @param prefix    The string to prefix console lines with.
   * @param timestamp The timestamp to prefix console lines with, defaults to the current time.
   * @param reports   The reports to print to the console.
   * @return An effect that prints the specified reports to the console.
   */
  def apply(
    prefix: String,
    timestamp: Option[OffsetDateTime] = None
  )(
    reports: Report[_]*
  ): URIO[Clock with Console, Unit] =
    for {
      actualTimestamp <- timestamp map (UIO(_)) getOrElse ZIO.accessM[Clock](_.clock.currentDateTime)
      message <- UIO {
        val result = new StringWriter()
        val out = new PrintWriter(result)
        val linePrefix = s"$prefix $actualTimestamp "
        reports.iterator flatMap (_.lines) foreach (out println linePrefix + _)
        out.flush()
        result.toString
      }
      _ <- if (message.isEmpty) UIO.unit else ZIO.accessM[Console](_.console.putStr(message))
    } yield ()

  /**
   * A report that contains a collection of indented lines.
   *
   * @tparam T The type of data to report.
   * @param indent The depth to indent this report.
   * @param input  The data to report.
   */
  case class Report[T: Reported](indent: Int, input: T) {

    /** The indented lines in this report. */
    lazy val lines: Iterator[String] = {
      val indention = if (indent <= 0) None else Some(" " * indent)

      Reported(input).linesIterator filterNot (_.trim.isEmpty) map { line =>
        indention map (i => s"$i$line") getOrElse line
      }
    }

  }

  /**
   * Implicit support for the report syntax.
   */
  object Report {

    /**
     * Treat any data as an unindented report.
     *
     * @tparam T The type of data to report.
     * @param data The data to report.
     * @return An unindented report for the specified data.
     */
    implicit def reportedToReport[T: Reported](data: T): Report[T] = Report(0, data)

    /** Treat any integer and input pair as a properly indented report. */
    implicit def indentedReportedToReport[T: Reported](indented: (Int, T)): Report[T] = Report(indented._1, indented._2)

  }

  /**
   * Type class that converts input data into reportable text.
   *
   * @tparam T The type of input that can be converted into reportable text.
   */
  trait Reported[-T] extends (T => String)

  /**
   * Implicit support for the reported types.
   */
  object Reported {

    /** Implicit support for reported strings. */
    implicit val string: Reported[String] = identity(_)

    /** Implicit support for reported throwables. */
    implicit val throwable: Reported[Throwable] = thrown => {
      val result = new StringWriter()
      val out = new PrintWriter(result)
      thrown.printStackTrace(out)
      out.flush()
      result.toString
    }

    /** Implicit support for reported causes. */
    implicit val cause: Reported[Cause[Any]] = _.prettyPrint

    /** Implicit support for optional reported data. */
    implicit def option[T: Reported]: Reported[Option[T]] = _ map apply[T] getOrElse ""

    def apply[T: Reported](input: T): String = implicitly[Reported[T]].apply(input)

  }

}
