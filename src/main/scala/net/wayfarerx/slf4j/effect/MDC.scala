/*
 * MDC.scala
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

import java.util.concurrent.atomic.{AtomicReference => JavaAtomicReference}

import cats.data.{NonEmptyList, NonEmptyMap}

import zio.{FiberRef, UIO, ZIO}

/**
 * A ZIO-specific implementation of SLF4J's mapped diagnostic context support.
 */
object MDC {

  /** The global cached fiber reference. */
  private val state = new CachedFiberRef

  /**
   * Returns the state of the underlying fiber's MDC.
   *
   * @return The state of the underlying fiber's MDC.
   */
  def apply(): UIO[Map[String, String]] =
    state() flatMap (_.get)

  /**
   * Wraps an effect with the specified entries applied to the MDC.
   *
   * @tparam R The environment required by the effect that uses the specified MDC entries.
   * @tparam E The type of error that can occur while using the specified MDC entries.
   * @tparam A The result of the effect that uses the specified MDC entries.
   * @param entry   The first entry to apply to the MDC.
   * @param entries The subsequent entries to apply to the MDC.
   * @param use     The effect to run with the specified MDC entries applied.
   * @return An effect with the specified entries applied to the MDC.
   */
  def apply[R, E, A](entry: (String, String), entries: (String, String)*)(use: ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      reference <- state()
      context <- reference.get
      result <- reference.locally(context ++ (entry +: entries).toMap)(use)
    } yield result

  /**
   * Wraps an effect with the specified entries applied to the MDC.
   *
   * @tparam R The environment required by the effect that uses the specified MDC entries.
   * @tparam E The type of error that can occur while using the specified MDC entries.
   * @tparam A The result of the effect that uses the specified MDC entries.
   * @param entries The entries to apply to the MDC.
   * @param use     The effect to run with the specified MDC entries applied.
   * @return An effect with the specified entries applied to the MDC.
   */
  def apply[R, E, A](entries: NonEmptyList[(String, String)])(use: ZIO[R, E, A]): ZIO[R, E, A] =
    apply(entries.head, entries.tail: _*)(use)

  /**
   * Wraps an effect with the specified entries applied to the MDC.
   *
   * @tparam R The environment required by the effect that uses the specified MDC entries.
   * @tparam E The type of error that can occur while using the specified MDC entries.
   * @tparam A The result of the effect that uses the specified MDC entries.
   * @param entries The entries to apply to the MDC.
   * @param use     The effect to run with the specified MDC entries applied.
   * @return An effect with the specified entries applied to the MDC.
   */
  def apply[R, E, A](entries: NonEmptyMap[String, String])(use: ZIO[R, E, A]): ZIO[R, E, A] =
    apply(entries.toNel)(use)

  /**
   * A utility that creates and caches a single fiber reference.
   *
   * @param create The effect that creates the fiber reference to use.
   */
  private[effect] final class CachedFiberRef(
    create: UIO[FiberRef[Map[String, String]]] = FiberRef.make(Map.empty[String, String])
  ) {

    /** The cached fiber reference. */
    private val cache = new JavaAtomicReference[Option[FiberRef[Map[String, String]]]](None)

    /**
     * Returns an effect that evaluates to the cached fiber reference.
     *
     * @return An effect that evaluates to the cached fiber reference.
     */
    def apply(): UIO[FiberRef[Map[String, String]]] = {

      /* Recursively resolve the singleton instance of the reference. */
      def resolve: UIO[FiberRef[Map[String, String]]] = for {
        current <- UIO(cache.get())
        result <- current map (UIO(_)) getOrElse {
          for {
            reference <- create
            activated <- UIO(cache.compareAndSet(None, Some(reference)))
            resolved <- if (activated) UIO(reference) else resolve
          } yield resolved
        }
      } yield result

      resolve
    }

  }

}
