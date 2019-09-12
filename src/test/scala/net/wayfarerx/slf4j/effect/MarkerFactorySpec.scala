/*
 * MarkerFactorySpec.scala
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

import zio.{DefaultRuntime, Task, UIO}
import zio.blocking.Blocking
import zio.console.Console

import org.scalamock.scalatest.MockFactory

import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

/**
 * Test suite for loggers.
 */
final class MarkerFactorySpec extends FlatSpec with Matchers with OneInstancePerTest with MockFactory {

  private val mockSlf4jMarkerFactory = mock[slf4j.IMarkerFactory]

  private val mockSlf4jMarker = mock[slf4j.Marker]

  private val mockSlf4jMarkerReference = mock[slf4j.Marker]

  private val mockSlf4jMarkerReference2 = mock[slf4j.Marker]

  private val mockConsole = mock[Console.Service[Any]]

  private val runtime = new DefaultRuntime {}

  private val thrown = new RuntimeException("thrown")

  "MarkerFactory" should "manage marker lifecycles" in {
    val markerFactory = MarkerFactory(MarkerFactory.Live(mockSlf4jMarkerFactory).markerFactory)
    (mockSlf4jMarker.getName _).expects().returning("marker").anyNumberOfTimes()
    (mockSlf4jMarkerReference.getName _).expects().returning("reference").anyNumberOfTimes()
    (mockSlf4jMarkerFactory.getMarker _).expects("marker").returning(mockSlf4jMarker).once()
    (mockSlf4jMarker.add _).expects(mockSlf4jMarkerReference).once()
    (mockSlf4jMarker.iterator _).expects()
      .returning(java.util.Arrays.asList(mockSlf4jMarkerReference).iterator()).once()
    (mockSlf4jMarker.remove _).expects(mockSlf4jMarkerReference).returning(true).once()
    (mockSlf4jMarkerFactory.detachMarker _).expects("marker").returning(true).once()
    runtime.unsafeRun(MarkerFactory("marker", new Marker(mockSlf4jMarkerReference)).provide(markerFactory)
      .use { marker =>
        for {
          _ <- Task(marker.name shouldBe "marker")
          _ <- marker.references flatMap (r => Task(r map (_.slf4jMarker) shouldBe Set(mockSlf4jMarkerReference)))
          result <- markerFactory.markerFactory("marker", new Marker(mockSlf4jMarkerReference)).use(m => UIO(m.name))
        } yield result
      }) shouldBe "marker"
  }

  it should "recover from failures while detaching markers" in {
    val markerFactory = MarkerFactory(MarkerFactory.Live(mockSlf4jMarkerFactory, console = mockConsole).markerFactory)
    (mockSlf4jMarker.getName _).expects().returning("marker").anyNumberOfTimes()
    (mockSlf4jMarkerFactory.getMarker _).expects("marker").returning(mockSlf4jMarker).once()
    (mockSlf4jMarkerFactory.detachMarker _).expects("marker").throwing(thrown).once()
    (mockConsole.putStr _).expects(where[String](_.startsWith("MARKER "))).returning(UIO.unit).once()
    runtime.unsafeRun(MarkerFactory("marker").provide(markerFactory).use(m => UIO(m.name))) shouldBe "marker"
  }

  it should "recover from failures while removing references" in {
    val markerFactory = MarkerFactory(MarkerFactory.Live(mockSlf4jMarkerFactory, console = mockConsole).markerFactory)
    (mockSlf4jMarker.getName _).expects().returning("marker").anyNumberOfTimes()
    (mockSlf4jMarkerReference.getName _).expects().returning("reference").anyNumberOfTimes()
    (mockSlf4jMarkerFactory.getMarker _).expects("marker").returning(mockSlf4jMarker).once()
    (mockSlf4jMarker.add _).expects(mockSlf4jMarkerReference).once()
    (mockSlf4jMarker.remove _).expects(mockSlf4jMarkerReference).throwing(thrown).once()
    (mockSlf4jMarkerFactory.detachMarker _).expects("marker").returning(true).once()
    (mockConsole.putStr _).expects(where[String](_.startsWith("MARKER "))).returning(UIO.unit).once()
    runtime.unsafeRun {
      MarkerFactory("marker", new Marker(mockSlf4jMarkerReference))
        .provide(markerFactory).use(m => UIO(m.name))
    } shouldBe "marker"
  }

  it should "react to failures when adding multiple references" in {
    val markerFactory = MarkerFactory(MarkerFactory.Live(mockSlf4jMarkerFactory, console = mockConsole).markerFactory)
    (mockSlf4jMarkerReference.getName _).expects().returning("reference").anyNumberOfTimes()
    (mockSlf4jMarkerReference2.getName _).expects().returning("reference2").anyNumberOfTimes()
    (mockSlf4jMarkerFactory.getMarker _).expects("marker").returning(mockSlf4jMarker).once()
    (mockSlf4jMarker.add _).expects(mockSlf4jMarkerReference).once()
    (mockSlf4jMarker.add _).expects(mockSlf4jMarkerReference2).throwing(thrown).once()
    (mockSlf4jMarker.remove _).expects(mockSlf4jMarkerReference).returning(true).once()
    (mockSlf4jMarkerFactory.detachMarker _).expects("marker").returning(true).once()
    runtime.unsafeRun {
      MarkerFactory("marker", new Marker(mockSlf4jMarkerReference), new Marker(mockSlf4jMarkerReference2))
        .provide(markerFactory).use(m => UIO(m.name)).fold(_.getMessage, identity)
    } shouldBe "thrown"
  }

  it should "construct live instances" in {
    runtime.unsafeRun(MarkerFactory.Live()) should not be null
    runtime.unsafeRun(MarkerFactory.Live(Blocking.Live.blocking)) should not be null
    runtime.unsafeRun(MarkerFactory.Live(console = Console.Live.console)) should not be null
    runtime.unsafeRun(MarkerFactory.Live(Blocking.Live.blocking, Console.Live.console)) should not be null
  }

}
