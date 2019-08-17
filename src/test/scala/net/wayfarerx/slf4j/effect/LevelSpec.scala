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

import org.scalatest.{FlatSpec, Matchers}

import org.slf4j.{event => slf4j}

/**
 * Test suite for the logging levels.
 */
final class LevelSpec extends FlatSpec with Matchers {

  "Level" should "convert to and from an integer" in {
    Level.Trace.toInt shouldBe slf4j.Level.TRACE.toInt
    Level(slf4j.Level.TRACE.toInt) shouldBe Some(Level.Trace)
    Level.Debug.toInt shouldBe slf4j.Level.DEBUG.toInt
    Level(slf4j.Level.DEBUG.toInt) shouldBe Some(Level.Debug)
    Level.Info.toInt shouldBe slf4j.Level.INFO.toInt
    Level(slf4j.Level.INFO.toInt) shouldBe Some(Level.Info)
    Level.Warn.toInt shouldBe slf4j.Level.WARN.toInt
    Level(slf4j.Level.WARN.toInt) shouldBe Some(Level.Warn)
    Level.Error.toInt shouldBe slf4j.Level.ERROR.toInt
    Level(slf4j.Level.ERROR.toInt) shouldBe Some(Level.Error)
    Level(-1) shouldBe None
  }

  it should "convert to and from a string" in {
    Level.Trace.toString() shouldBe slf4j.Level.TRACE.toString()
    Level(slf4j.Level.TRACE.toString()) shouldBe Some(Level.Trace)
    Level.Debug.toString() shouldBe slf4j.Level.DEBUG.toString()
    Level(slf4j.Level.DEBUG.toString()) shouldBe Some(Level.Debug)
    Level.Info.toString() shouldBe slf4j.Level.INFO.toString()
    Level(slf4j.Level.INFO.toString()) shouldBe Some(Level.Info)
    Level.Warn.toString() shouldBe slf4j.Level.WARN.toString()
    Level(slf4j.Level.WARN.toString()) shouldBe Some(Level.Warn)
    Level.Error.toString() shouldBe slf4j.Level.ERROR.toString()
    Level(slf4j.Level.ERROR.toString()) shouldBe Some(Level.Error)
    Level("") shouldBe None
  }

  it should "convert to and from a SLF4J logging level" in {
    Level.Trace.slf4jLevel shouldBe slf4j.Level.TRACE
    Level(slf4j.Level.TRACE) shouldBe Level.Trace
    Level.Debug.slf4jLevel shouldBe slf4j.Level.DEBUG
    Level(slf4j.Level.DEBUG) shouldBe Level.Debug
    Level.Info.slf4jLevel shouldBe slf4j.Level.INFO
    Level(slf4j.Level.INFO) shouldBe Level.Info
    Level.Warn.slf4jLevel shouldBe slf4j.Level.WARN
    Level(slf4j.Level.WARN) shouldBe Level.Warn
    Level.Error.slf4jLevel shouldBe slf4j.Level.ERROR
    Level(slf4j.Level.ERROR) shouldBe Level.Error
  }

}
