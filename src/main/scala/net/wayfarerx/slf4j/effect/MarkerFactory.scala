/*
 * MarkerFactory.scala
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

import zio.{RIO, Semaphore, Task, UIO, URIO, ZManaged}
import zio.blocking.Blocking
import zio.console.Console

/**
 * Environment mix-in that exposes the marker factory API.
 */
trait MarkerFactory {

  /** The exposed marker factory API. */
  val markerFactory: MarkerFactoryApi.Aux[Any]

}

/**
 * The global marker factory service and definitions that support the `MarkerFactory` environment.
 */
object MarkerFactory extends (MarkerFactoryApi.Aux[Any] => MarkerFactory) with MarkerFactoryApi.Service[MarkerFactory] {

  /**
   * Creates a new marker factory from the specified marker factory service.
   *
   * @param markerFactory The marker factory service to use in the new marker factory.
   * @return A new marker factory from the specified marker factory service.
   */
  override def apply(markerFactory: MarkerFactoryApi.Aux[Any]): MarkerFactory = {
    val _markerFactory = markerFactory
    new MarkerFactory {
      override val markerFactory = _markerFactory
    }
  }

  /* Return a managed reference to the specified marker. */
  override def get(name: String, references: Set[Marker]): Result[Marker] =
    ZManaged.fromFunctionM(_.markerFactory.get(name, references))

  /**
   * Implementation of the `MarkerFactory` environment using a SLF4J `IMarkerFactory`.
   */
  trait Live extends MarkerFactory {
    self: Blocking with Console =>

    /** The underlying SLF4J `IMarkerFactory`. */
    val slf4jMarkerFactory: slf4j.IMarkerFactory

    /* Implement the marker factory API. */
    final override val markerFactory: MarkerFactoryApi.Aux[Any] = new MarkerFactoryApi.Service[Any] {

      import Live.MarkerInstance

      /** The index of active marker entries and reference counts by name. */
      private var instances = Map[String, (Live.MarkerInstance, Long)]()

      /* Return a managed reference to the specified marker. */
      override def get(name: String, references: Set[Marker]) = ZManaged.make {
        Semaphore make 1L flatMap { semaphore =>
          blocking effectBlocking synchronized {
            val (instance, count) = instances.getOrElse(name,
              new MarkerInstance(slf4jMarkerFactory.getMarker(name), semaphore) -> 0L
            )
            instances += name -> (instance, count + 1L)
            instance
          }
        }
      } { _ =>
        blocking.effectBlocking {
          synchronized {
            instances get name foreach { case (instance, count) =>
              if (count > 1L) instances += name -> (instance, count - 1L) else {
                instances -= name
                slf4jMarkerFactory.detachMarker(name)
              }
            }
          }
        }.foldCauseM(
          failure => Recover("MARKER")(
            s"""Failed to detach SLF4J marker "$name":""",
            2 -> failure
          ).provide(self),
          _ => UIO.unit
        )
      } flatMap (_ (references.toList) provide self) map (new Marker(_))

    }

  }

  /**
   * Factory for live marker factory environments.
   */
  object Live extends ((
    slf4j.IMarkerFactory,
      Blocking.Service[Any],
      Console.Service[Any]
    ) => Live with Blocking with Console) {

    /**
     * Creates a live `LoggerFactory`.
     *
     * @return A live `LoggerFactory`.
     */
    def apply(): Task[Live with Blocking with Console] =
      Task(slf4j.MarkerFactory.getIMarkerFactory) map (Live(_))

    /**
     * Creates a live `MarkerFactory` with the specified blocking service.
     *
     * @param blocking The blocking service to use.
     * @return A live `MarkerFactory` with the specified blocking service.
     */
    def apply(blocking: Blocking.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.MarkerFactory.getIMarkerFactory) map (Live(_, blocking))

    /**
     * Creates a live `MarkerFactory` with the specified console service.
     *
     * @param console The console service to use.
     * @return A live `MarkerFactory` with the specified console service.
     */
    def apply(console: Console.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.MarkerFactory.getIMarkerFactory) map (Live(_, console = console))

    /**
     * Creates a live `MarkerFactory` with the specified blocking and console service.
     *
     * @param blocking The blocking service to use.
     * @param console  The console service to use.
     * @return A live `MarkerFactory` with the specified blocking and console service.
     */
    def apply(blocking: Blocking.Service[Any], console: Console.Service[Any]): Task[Live with Blocking with Console] =
      Task(slf4j.MarkerFactory.getIMarkerFactory) map (Live(_, blocking, console))

    /**
     * Creates a live `MarkerFactory` implementation with the specified blocking and console service.
     *
     * @param slf4jMarkerFactory The SLF4J marker factory to use.
     * @param blocking           The blocking service to use, defaults to the global blocking service.
     * @param console            The console service to use, defaults to the global console service.
     * @return A live `MarkerFactory` implementation with the specified blocking and console service.
     */
    override def apply(
      slf4jMarkerFactory: slf4j.IMarkerFactory,
      blocking: Blocking.Service[Any] = Blocking.Live.blocking,
      console: Console.Service[Any] = Console.Live.console
    ): Live with Blocking with Console = {
      val _slf4jMarkerFactory = slf4jMarkerFactory
      val _blocking = blocking
      val _console = console
      new Live with Blocking with Console {
        override val slf4jMarkerFactory = _slf4jMarkerFactory
        override val blocking = _blocking
        override val console = _console
      }
    }

    /**
     * A single marker entry that provides a managed SLF4J marker.
     *
     * @param slf4jMarker The underlying SLF4J marker.
     */
    private final class MarkerInstance(slf4jMarker: slf4j.Marker, semaphore: Semaphore) {

      /** The index of active referenced markers and reference count by name. */
      @volatile private var referenceCounts = Map[String, Long]()

      /**
       * Returns a managed marker with the specified references applied.
       *
       * @param references The references to apply to the marker.
       * @return A managed marker with the specified references applied.
       */
      def apply(references: List[Marker]): ZManaged[Console, Throwable, slf4j.Marker] = {
        if (references.isEmpty) ZManaged.succeed(())
        else ZManaged.make(semaphore withPermit add(references))(_ => semaphore withPermit remove(references.reverse))
      } map (_ => slf4jMarker)

      /**
       * Adds the specified references to the underlying marker only if all references can be added.
       *
       * @param remaining The remaining references to add.
       * @return An effect that adds the specified references to the underlying marker.
       */
      private def add(remaining: List[Marker]): RIO[Console, Unit] = remaining match {
        case head :: tail => Task {
          val count = referenceCounts.getOrElse(head.name, 0L)
          if (count < 1L) slf4jMarker.add(head.slf4jMarker)
          referenceCounts += head.name -> (count + 1L)
        } flatMap (_ => add(tail) onError (_ => removing(head)))
        case Nil => UIO.unit
      }

      /**
       * Removes the specified references from the underlying marker.
       *
       * @param remaining The remaining references to remove.
       * @return An effect that removes the specified references from the underlying marker.
       */
      private def remove(remaining: List[Marker]): URIO[Console, Unit] = remaining match {
        case head :: tail => removing(head) ensuring remove(tail)
        case Nil => UIO.unit
      }

      /**
       * Implementation of failure-proof marker reference removal.
       *
       * @param reference The reference to remove from the marker.
       * @return An effect that removes a marker reference while handling any errors.
       */
      private def removing(reference: Marker): URIO[Console, Unit] = Task {
        val currentCount = referenceCounts get reference.name
        val count = currentCount getOrElse 0L
        if (count <= 1L) {
          referenceCounts -= reference.name
          slf4jMarker.remove(reference.slf4jMarker)
        } else referenceCounts += reference.name -> (count - 1L)
      }.foldCauseM(
        failure => Recover("MARKER")(
          s"""Failed to remove reference "${reference.name}" from SLF4J marker "${slf4jMarker.getName}":""",
          2 -> failure
        ),
        _ => UIO.unit
      )

    }

  }

}
