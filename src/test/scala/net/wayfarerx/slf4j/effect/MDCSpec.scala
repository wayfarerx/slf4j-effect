/*
 * MDCSpec.scala
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

import cats.data.NonEmptyMap
import cats.implicits._
import zio.{DefaultRuntime, FiberRef, UIO}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for the MDC API.
 */
final class MDCSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val runtime = new DefaultRuntime {}

  "MDC" should "manage nested mapped diagnostic contexts" in {
    runtime.unsafeRun {
      for {
        _ <- MDC() map (_ shouldBe Map.empty)
        result <- MDC("first" -> "1") {
          for {
            _ <- MDC() map (_ shouldBe Map("first" -> "1"))
            _ <- MDC(NonEmptyMap.of("second" -> "2")) {
              MDC() map (_ shouldBe Map("first" -> "1", "second" -> "2"))
            }
          } yield true
        }
      } yield result
    } shouldBe true
  }

  it should "recursively locate the global fiber reference" in {
    var cached: Option[MDC.State] = None
    cached = Some(new MDC.State({
      var reentering = false
      UIO(reentering) flatMap { r =>
        if (r) FiberRef.make(Map.empty[String, String]) else for {
          _ <- UIO {
            reentering = true
          }
          _ <- cached.get()
          result <- FiberRef.make(Map.empty[String, String])
        } yield result
      }
    }))
    runtime.unsafeRun(cached.get()) should not be null
  }

}
