/*
 * MarkerSpec.scala
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

import zio.DefaultRuntime

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for the marker API.
 */
final class MarkerSpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockSlf4jMarker = mock[slf4j.Marker]

  private val mockSlf4jMarkerReference = mock[slf4j.Marker]

  private val runtime = new DefaultRuntime {}

  "Marker" should "provide a pure marker API" in {
    (mockSlf4jMarker.getName _).expects()
      .returning("markerName").once()
    (mockSlf4jMarker.iterator _).expects()
      .returning(java.util.Collections.singletonList(mockSlf4jMarkerReference).iterator())
    (mockSlf4jMarker.contains(_: slf4j.Marker)).expects(mockSlf4jMarkerReference)
      .returning(true)
    (mockSlf4jMarker.contains(_: String)).expects("missing")
      .returning(false)
    val marker = new Marker(mockSlf4jMarker)
    marker.name shouldBe "markerName"
    runtime.unsafeRun(marker.references) shouldBe Set(new Marker(mockSlf4jMarkerReference))
    runtime.unsafeRun(marker.contains(new Marker(mockSlf4jMarkerReference))) shouldBe true
    runtime.unsafeRun(marker.contains("missing")) shouldBe false
    marker.toString() shouldBe mockSlf4jMarker.toString()
  }

}
