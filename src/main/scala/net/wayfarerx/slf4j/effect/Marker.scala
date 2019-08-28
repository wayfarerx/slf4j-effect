/*
 * Marker.scala
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

import zio.UIO

/**
 * Definition of marker, a named object used to enrich log statements.
 *
 * @param slf4jMarker The underlying SLF4J marker.
 */
final class Marker(val slf4jMarker: slf4j.Marker) extends AnyVal {

  /**
   * The name of this marker.
   *
   * NOTE: This assumes that `org.slf4j.Marker.getName()` is pure.
   */
  def name: String = slf4jMarker.getName

  /**
   * The references defined on this marker.
   *
   * NOTE: This assumes that `org.slf4j.Marker.iterator()`, and its resulting `java.util.Iterator` do not fail.
   */
  def references: UIO[Set[Marker]] = UIO {
    val references = slf4jMarker.iterator()
    new Iterator[Marker] {

      override def hasNext = references.hasNext

      override def next() = new Marker(references.next())

    }.toSet
  }

  /**
   * Returns true if this marker recursively references the other marker.
   *
   * NOTE: This assumes that `org.slf4j.Marker.contains(org.slf4j.Marker)` does not fail.
   *
   * @param other The marker to test for inclusion.
   * @return True if this marker recursively references the other marker.
   */
  def contains(other: Marker): UIO[Boolean] =
    UIO(slf4jMarker contains other.slf4jMarker)

  /**
   * Returns true if this marker recursively references a marker with the specified name.
   *
   * NOTE: This assumes that `org.slf4j.Marker.contains(java.lang.String)` does not fail.
   *
   * @param name The name of the marker to test for inclusion.
   * @return True if this marker recursively references a marker with the specified name.
   */
  def contains(name: String): UIO[Boolean] =
    UIO(slf4jMarker contains name)

  /*
   * Convert the underlying SLF4J marker into a string.
   *
   * NOTE: This assumes that `org.slf4j.Marker.toString()` is pure.
   */
  override def toString(): String =
    slf4jMarker.toString()

}
