/*
 * Level.scala
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

import org.slf4j.{event => slf4j}

/**
 * Base type for the supported logging levels.
 */
sealed abstract class Level(val slf4jLevel: slf4j.Level) extends Product {

  /** Returns the SLF4J logging level integer. */
  def toInt: Int = slf4jLevel.toInt

  /* Return the SLF4J logging level string. */
  override def toString: String = slf4jLevel.toString

}

/**
 * Support for the `Level` type.
 */
object Level extends (slf4j.Level => Level) {

  /** All of the supported logging levels. */
  lazy val Levels: Set[Level] =
    Set(Trace, Debug, Info, Warn, Error)

  /** Logging levels indexed by SLF4J `Level`. */
  private lazy val levelsBySlf4jLevel: Map[slf4j.Level, Level] =
    Levels.iterator.map(l => l.slf4jLevel -> l).toMap

  /** Logging levels indexed by SLF4J `Level` integer. */
  private lazy val levelsBySlf4jLevelInt: Map[Int, Level] =
    Levels.iterator.map(l => l.toInt -> l).toMap

  /** Logging levels indexed by SLF4J `Level` string. */
  private lazy val levelsBySlf4jLevelStr: Map[String, Level] =
    Levels.iterator.map(l => l.toString().toUpperCase -> l).toMap

  /**
   * Returns the logging level for the specified SLF4J `Level`.
   *
   * @param slf4jLevel The SLF4J `Level` to return the logging level for.
   * @return The logging level for the specified SLF4J `Level`.
   */
  override def apply(slf4jLevel: slf4j.Level): Level =
    levelsBySlf4jLevel(slf4jLevel)

  /**
   * Returns the logging level for the specified SLF4J `Level` integer.
   *
   * @param slf4jLevelInt The SLF4J `Level` integer to return the logging level for.
   * @return The logging level for the specified SLF4J `Level` integer.
   */
  def apply(slf4jLevelInt: Int): Option[Level] =
    levelsBySlf4jLevelInt get slf4jLevelInt

  /**
   * Returns the logging level for the specified SLF4J `Level` string.
   *
   * @param slf4jLevelStr The SLF4J `Level` string to return the logging level for.
   * @return The logging level for the specified SLF4J `Level` string.
   */
  def apply(slf4jLevelStr: String): Option[Level] =
    levelsBySlf4jLevelStr get slf4jLevelStr.toUpperCase

  /** The `TRACE` logging level. */
  case object Trace extends Level(slf4j.Level.TRACE)

  /** The `DEBUG` logging level. */
  case object Debug extends Level(slf4j.Level.DEBUG)

  /** The `INFO` logging level. */
  case object Info extends Level(slf4j.Level.INFO)

  /** The `WARN` logging level. */
  case object Warn extends Level(slf4j.Level.WARN)

  /** The `ERROR` logging level. */
  case object Error extends Level(slf4j.Level.ERROR)

}
