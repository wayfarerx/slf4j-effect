/*
 * LevelSpec.scala
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

import org.scalatest.{FlatSpec, Matchers}

/**
 * Test suite for the supported logging levels.
 */
final class LevelSpec extends FlatSpec with Matchers {

  "Level" should "convert to and from an integer" in {
    Level.Trace.toInt shouldBe Slf4jLevel.TRACE.toInt
    Level(Slf4jLevel.TRACE.toInt) shouldBe Some(Level.Trace)
    Level.Debug.toInt shouldBe Slf4jLevel.DEBUG.toInt
    Level(Slf4jLevel.DEBUG.toInt) shouldBe Some(Level.Debug)
    Level.Info.toInt shouldBe Slf4jLevel.INFO.toInt
    Level(Slf4jLevel.INFO.toInt) shouldBe Some(Level.Info)
    Level.Warn.toInt shouldBe Slf4jLevel.WARN.toInt
    Level(Slf4jLevel.WARN.toInt) shouldBe Some(Level.Warn)
    Level.Error.toInt shouldBe Slf4jLevel.ERROR.toInt
    Level(Slf4jLevel.ERROR.toInt) shouldBe Some(Level.Error)
    Level(-1) shouldBe None
  }

  it should "convert to and from a string" in {
    Level.Trace.toString() shouldBe Slf4jLevel.TRACE.toString
    Level(Slf4jLevel.TRACE.toString) shouldBe Some(Level.Trace)
    Level.Debug.toString() shouldBe Slf4jLevel.DEBUG.toString
    Level(Slf4jLevel.DEBUG.toString) shouldBe Some(Level.Debug)
    Level.Info.toString() shouldBe Slf4jLevel.INFO.toString
    Level(Slf4jLevel.INFO.toString) shouldBe Some(Level.Info)
    Level.Warn.toString() shouldBe Slf4jLevel.WARN.toString
    Level(Slf4jLevel.WARN.toString) shouldBe Some(Level.Warn)
    Level.Error.toString() shouldBe Slf4jLevel.ERROR.toString
    Level(Slf4jLevel.ERROR.toString) shouldBe Some(Level.Error)
    Level("") shouldBe None
  }

  it should "convert to and from a SLF4J logging level" in {
    Level.Trace.slf4jLevel shouldBe Slf4jLevel.TRACE
    Level(Slf4jLevel.TRACE) shouldBe Level.Trace
    Level.Debug.slf4jLevel shouldBe Slf4jLevel.DEBUG
    Level(Slf4jLevel.DEBUG) shouldBe Level.Debug
    Level.Info.slf4jLevel shouldBe Slf4jLevel.INFO
    Level(Slf4jLevel.INFO) shouldBe Level.Info
    Level.Warn.slf4jLevel shouldBe Slf4jLevel.WARN
    Level(Slf4jLevel.WARN) shouldBe Level.Warn
    Level.Error.slf4jLevel shouldBe Slf4jLevel.ERROR
    Level(Slf4jLevel.ERROR) shouldBe Level.Error
  }

}
