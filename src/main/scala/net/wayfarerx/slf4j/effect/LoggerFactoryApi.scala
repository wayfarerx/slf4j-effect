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

import zio.RIO

import scala.reflect.ClassTag

/**
 * Definition of the API for accessing SLF4J logger factories.
 *
 * @tparam R The environment type to require in results.
 */
trait LoggerFactoryApi[-R] {

  /** The type of result returned by this API. */
  type Result[+A] <: RIO[R, A]

  /**
   * Returns a logger named according to the name parameter.
   *
   * @param name The name of the logger.
   * @return A logger named according to the name parameter.
   */
  def apply(name: String): Result[LoggerOld]

  /**
   * Returns a logger named corresponding to the class passed as parameter.
   *
   * @tparam T The type of the class to name the logger after.
   * @return A logger named corresponding to the class passed as parameter.
   */
  final def apply[T: ClassTag](): Result[LoggerOld] =
    apply(implicitly[ClassTag[T]].runtimeClass.getName)

}

/**
 * Definitions that support the `LoggerFactoryApi` trait.
 */
object LoggerFactoryApi {

  /**
   * Base type for logger factory API implementations.
   *
   * @tparam R The environment type to require in results.
   */
  trait Service[R] extends LoggerFactoryApi[R] {

    /* Configure the type of result returned by this API. */
    final override type Result[+A] = RIO[R, A]

  }

}
