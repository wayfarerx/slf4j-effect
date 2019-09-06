/*
 * LoggerFactoryApi.scala
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

import zio.URIO

/**
 * Definition of the API for accessing SLF4J logger factories.
 *
 * @tparam R The environment type to require in results.
 */
trait LoggerFactoryApi[-R] {

  /** The type of result returned by this API. */
  type Result[+A] <: URIO[R, A]

  /**
   * Returns a logger named according to the name parameter.
   *
   * @param name The name of the logger.
   * @return A logger named according to the name parameter.
   */
  def apply(name: String): Result[Logger]

  /**
   * Returns a logger named corresponding to the class passed as parameter
   *
   * @tparam T The type of the specified class.
   * @param cls The class to name the returned logger after.
   * @return A logger named corresponding to the class passed as parameter
   */
  final def apply[T](cls: Class[T]): Result[Logger] =
    apply(cls.getName)

}

/**
 * Definitions that support the `LoggerFactoryApi` trait.
 */
object LoggerFactoryApi {

  /** The type of logger factory APIs with a fixed environment. */
  type Aux[R] = LoggerFactoryApi[R] {type Result[+A] = URIO[R, A]}

  /**
   * Base type for logger factory API implementations.
   *
   * @tparam R The environment type to require in results.
   */
  trait Service[R] extends LoggerFactoryApi[R] {

    /* Configure the type of result returned by this API. */
    final override type Result[+A] = URIO[R, A]

  }

}
