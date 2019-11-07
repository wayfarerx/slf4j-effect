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

import zio.{DefaultRuntime, Runtime}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console

/**
 * Global definitions for the SLF4J-effect package.
 */
package object effect {

  /** The ZIO runtime to use in SLF4J-effect. */
  private[effect] lazy val slf4jEffectRuntime: Runtime[Blocking with Clock with Console] =
    new DefaultRuntime {} // FIXME

}
