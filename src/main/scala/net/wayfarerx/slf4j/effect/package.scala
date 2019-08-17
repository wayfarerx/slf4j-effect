/*
 * package.scala
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

package net.wayfarerx.slf4j

import language.higherKinds

import zio.{URIO, ZIO}

/**
 * Global access to the SLF4J logger operations.
 */
package object effect extends LoggerApi[Logger] {

  /* This API's effects are bound to a `Logger`. */
  override type Effect[+A] = URIO[Logger, A]

  /* Return true if the specified logging level is enabled. */
  override def isEnabled(level: Level): Effect[Boolean] =
    ZIO.accessM[Logger](_.logger.isEnabled(level))

  /* Log a message at the specified level. */
  override def log(level: Level, f: => String): Effect[Unit] =
    ZIO.accessM[Logger](_.logger.log(level, f))

  /* Log a message and `Throwable` at the specified level. */
  override def log(level: Level, f: => String, t: Throwable): Effect[Unit] =
    ZIO.accessM[Logger](_.logger.log(level, f, t))

}
