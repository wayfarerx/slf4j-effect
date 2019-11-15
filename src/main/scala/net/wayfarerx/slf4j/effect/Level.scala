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

import org.slf4j.event.{Level => Slf4jLevel}

/**
 * Base type for the supported logging levels.
 *
 * @param slf4jLevel The supported SLF4J logging level.
 */
sealed abstract class Level(val slf4jLevel: Slf4jLevel) extends Product {

  /** Returns the SLF4J logging level integer. */
  @inline final def toInt: Int = slf4jLevel.toInt

  /** Returns the SLF4J logging level string. */
  @inline final override def toString: String = slf4jLevel.toString

}

/**
 * Implementations of the supported logging levels.
 */
object Level extends (Slf4jLevel => Level) {

  /** All of the supported logging levels. */
  lazy val Levels: Set[Level] = Set(Trace, Debug, Info, Warn, Error)

  /** The supported logging levels indexed by SLF4J `Level`. */
  private lazy val LevelsBySlf4jLevel: Map[Slf4jLevel, Level] =
    Levels.iterator.map(l => l.slf4jLevel -> l).toMap

  /** The supported logging levels indexed by SLF4J `Level` integer. */
  private lazy val LevelsByInt: Map[Int, Level] =
    Levels.iterator.map(l => l.toInt -> l).toMap

  /** The supported logging levels indexed by SLF4J `Level` string. */
  private lazy val LevelsByString: Map[String, Level] =
    Levels.iterator.map(l => l.toString().toUpperCase -> l).toMap

  /**
   * Returns the logging level for the specified SLF4J `Level`.
   *
   * @param slf4jLevel The SLF4J `Level` to return the logging level for.
   * @return The logging level for the specified SLF4J `Level`.
   */
  override def apply(slf4jLevel: Slf4jLevel): Level =
    LevelsBySlf4jLevel(slf4jLevel)

  /**
   * Returns the logging level for the specified SLF4J `Level` integer if one exists.
   *
   * @param slf4jLevelInt The SLF4J `Level` integer to return the logging level for.
   * @return The logging level for the specified SLF4J `Level` integer if one exists.
   */
  def apply(slf4jLevelInt: Int): Option[Level] =
    LevelsByInt get slf4jLevelInt

  /**
   * Returns the logging level for the specified SLF4J `Level` string if one exists.
   *
   * @param slf4jLevelStr The SLF4J `Level` string to return the logging level for.
   * @return The logging level for the specified SLF4J `Level` string if one exists.
   */
  def apply(slf4jLevelStr: String): Option[Level] =
    LevelsByString get slf4jLevelStr.toUpperCase

  /** The `TRACE` logging level. */
  case object Trace extends Level(Slf4jLevel.TRACE)

  /** The `DEBUG` logging level. */
  case object Debug extends Level(Slf4jLevel.DEBUG)

  /** The `INFO` logging level. */
  case object Info extends Level(Slf4jLevel.INFO)

  /** The `WARN` logging level. */
  case object Warn extends Level(Slf4jLevel.WARN)

  /** The `ERROR` logging level. */
  case object Error extends Level(Slf4jLevel.ERROR)

}
