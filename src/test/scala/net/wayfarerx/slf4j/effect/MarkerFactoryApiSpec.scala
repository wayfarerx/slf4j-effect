/*
 * MarkerFactoryApiSpec.scala
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

import org.slf4j
import zio.{DefaultRuntime, UIO, ZManaged}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for the marker factory API operations.
 */
final class MarkerFactoryApiSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockSlf4jMarker = mock[slf4j.Marker]

  private val mockSlf4jMarkerReference = mock[slf4j.Marker]

  private val mockMarkerFactory = mock[MarkerFactoryApi.Service[Any]]

  private val runtime = new DefaultRuntime {}

  "MarkerFactoryApi" should "create managed markers" in {
    (mockMarkerFactory.get _).expects("marker", Set(new Marker(mockSlf4jMarkerReference))).once()
      .returns(ZManaged.succeed(new Marker(mockSlf4jMarker)))
    runtime.unsafeRun(mockMarkerFactory("marker", new Marker(mockSlf4jMarkerReference)).use(UIO(_))) shouldBe
      new Marker(mockSlf4jMarker)
  }

}
