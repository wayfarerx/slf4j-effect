/*
 * MarkerFactoryApi.scala
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

import language.higherKinds

import zio.ZManaged

/**
 * Definition of the API for accessing SLF4J marker factories.
 *
 * @tparam R The environment type to require in results.
 */
trait MarkerFactoryApi[-R] {

  /** The type of effect returned by this API. */
  type Result[+A] <: ZManaged[R, Throwable, A]

  /**
   * Returns a managed reference to the specified marker.
   *
   * @param name       The name of the marker to supply.
   * @param references The references to apply to the marker.
   * @return A managed reference to the specified marker.
   */
  final def apply(name: String, references: Marker*): Result[Marker] =
    get(name, references.toSet)

  /**
   * Returns a managed reference to the specified marker.
   *
   * @param name       The name of the marker to return.
   * @param references The references to apply to the marker.
   * @return A managed reference to the specified marker.
   */
  def get(name: String, references: Set[Marker]): Result[Marker]

}

/**
 * Definitions that support the `MarkerFactoryApi` trait.
 */
object MarkerFactoryApi {

  /** The type of marker factory APIs with a fixed environment. */
  type Aux[R] = MarkerFactoryApi[R] {type Result[+A] = ZManaged[R, Throwable, A]}

  /**
   * Base type for marker factory API implementations.
   *
   * @tparam R The environment type to require in results.
   */
  trait Service[R] extends MarkerFactoryApi[R] {

    /* Configure the type of effect returned by this API. */
    final override type Result[+A] = ZManaged[R, Throwable, A]

  }

}
