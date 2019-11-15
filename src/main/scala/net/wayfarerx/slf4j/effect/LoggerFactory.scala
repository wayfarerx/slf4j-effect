/*
 * Logger.scala
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

import cats.Monad

import language.higherKinds
import reflect.ClassTag
import cats.implicits._
import cats.effect.{Effect, LiftIO, Sync}
import org.slf4j
import zio.{Runtime, Task}
import zio.interop.catz._

/**
 *
 * @tparam F The type of effect that this logger factory uses.
 */
trait LoggerFactory[F[_]] {

  /**
   * Returns a logger named according to the name parameter.
   *
   * @param name The name of the logger.
   * @return A logger named according to the name parameter.
   */
  def apply(name: String): F[Logger[F]]

  /**
   * Returns a logger named corresponding to the class passed as parameter.
   *
   * @tparam T The type of the class to name the logger after.
   * @return A logger named corresponding to the class passed as parameter.
   */
  @inline final def apply[T: ClassTag](): F[Logger[F]] =
    apply(implicitly[ClassTag[T]].runtimeClass.getName)

}

/**
 * The global SLF4J logger factory constructors and related definitions.
 */
object LoggerFactory {

  final private lazy val taskMonad = implicitly[Monad[Task]]

  final private lazy val taskLiftIO = implicitly[LiftIO[Task]]

  /**
   * Creates a ZIO logger factory that wraps the global SLF4J logger factory.
   *
   * @return A ZIO logger factory that wraps the global SLF4J logger factory.
   */
  def apply(): Task[Service[Task]] =
    Task(slf4j.LoggerFactory.getILoggerFactory) map apply

  /**
   * Creates a ZIO logger factory that wraps the specified SLF4J logger factory.
   *
   * @param slf4jLoggerFactory The SLF4J logger factory to wrap.
   * @return A ZIO logger factory that wraps the specified SLF4J logger factory.
   */
  def apply(slf4jLoggerFactory: slf4j.ILoggerFactory): Service[Task] = {
    val _slf4jLoggerFactory = slf4jLoggerFactory
    new Service[Task] {

      override def monad: Monad[Task] = taskMonad

      override def liftIO: LiftIO[Task] = taskLiftIO

      final override def slf4jLoggerFactory = _slf4jLoggerFactory

      final override protected def lift[A](action: Task[A]) = action

    }
  }

  /**
   * Creates a cats-effect logger factory that wraps the global SLF4J logger factory.
   *
   * @tparam F The type of effect that the logger factory will use.
   * @param runtime The optional ZIO runtime to use.
   * @return A cats-effect logger factory that wraps the global SLF4J logger factory.
   */
  def using[F[_] : Sync : LiftIO](runtime: Runtime[Any] = slf4jEffectRuntime): F[Service[F]] =
    implicitly[Sync[F]] delay slf4j.LoggerFactory.getILoggerFactory map (using[F](_, runtime))

  /**
   * Creates a cats-effect logger factory that wraps the specified SLF4J logger factory.
   *
   * @tparam F The type of effect that the logger factory will use.
   * @param slf4jLogger The SLF4J logger factory to wrap.
   * @param runtime     The optional ZIO runtime to use.
   * @return A cats-effect logger factory that wraps the specified SLF4J logger factory.
   */
  def using[F[_] : Monad : LiftIO](
    slf4jLogger: slf4j.ILoggerFactory,
    runtime: Runtime[Any] = slf4jEffectRuntime
  ): Service[F] = {
    val _slf4jLogger = slf4jLogger
    implicit val _runtime: Runtime[Any] = runtime
    val taskEffect = implicitly[Effect[Task]]
    new Service[F] {

      override def monad: Monad[F] = implicitly[Monad[F]]

      override def liftIO: LiftIO[F] = implicitly[LiftIO[F]]

      override def slf4jLoggerFactory = _slf4jLogger

      override protected def lift[A](action: Task[A]) =
        implicitly[LiftIO[F]].liftIO(taskEffect.toIO(action))

    }
  }

  /**
   * Base class for effectful `LoggerFactory` implementations on top of the SLF4J API.
   *
   * @tparam F The type of effect that this logger factory uses.
   */
  trait Service[F[_]] extends LoggerFactory[F] {

    /** The monad that describes the effect type. */
    implicit def monad: Monad[F]

    /** The IO lifter that describes the effect type. */
    implicit def liftIO: LiftIO[F]

    /** The underlying SLF4J logger factory to use. */
    def slf4jLoggerFactory: slf4j.ILoggerFactory

    /* Return a logger named according to the name parameter. */
    final override def apply(name: String): F[Logger[F]] =
      lift(Task(slf4jLoggerFactory.getLogger(name)) map (Logger.using[F](_)))

    /**
     * Lift an stand alone ZIO effect into the underlying effect type.
     *
     * @tparam A The type of result produced by the ZIO effect.
     * @param action The ZIO action to lift into the monadic type.
     * @return The specified ZIO effect lifted into the underlying monadic type.
     */
    protected def lift[A](action: Task[A]): F[A]

  }

}