package com.sbuslab.utils

import scala.language.higherKinds

import scala.collection.mutable
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.ActorSystem

import com.sbuslab.model.UnrecoverableError


object FutureHelpers extends FutureHelpers {

  def serial[A, B](in: Seq[A])(f: A ⇒ Future[B])(implicit ec: ExecutionContext): Future[Seq[B]] =
    in.foldLeft(Future.successful(Seq.newBuilder[B])) { case (fr, a) ⇒
      for (result ← fr; r ← f(a)) yield result += r
    } map (_.result())

  def collectWhile[A, B, M[X] <: Seq[X]](in: M[Future[A]])(pf: PartialFunction[A, B])(implicit cbf: CanBuildFrom[M[Future[A]], B, M[B]], ec: ExecutionContext): Future[M[B]] =
    collectWhileImpl(in, pf, cbf(in)).map(_.result())

  private def collectWhileImpl[A, B, M[X] <: Seq[X]](in: M[Future[A]], pf: PartialFunction[A, B], buffer: mutable.Builder[B, M[B]])(implicit ec: ExecutionContext): Future[mutable.Builder[B, M[B]]] =
    if (in.isEmpty) {
      Future.successful(buffer)
    } else {
      in.head flatMap {
        case r if pf.isDefinedAt(r) ⇒ collectWhileImpl(in.tail.asInstanceOf[M[Future[A]]], pf, buffer += pf(r))
        case _ ⇒ Future.successful(buffer)
      }
    }
}


trait FutureHelpers {

  implicit def richFuture[T](future: Future[T]): RichFuture[T] = new RichFuture(future)

  object Retry {

    def max[T](maxAttempts: Int = 5, noRetry: PartialFunction[Throwable, Boolean] = retryAll)(f: ⇒ Future[T])(implicit system: ActorSystem, ec: ExecutionContext): Future[T] = {
      expBackOff(maxAttempts, noRetry)(f)
    }

    def expBackOff[T](maxAttempts: Int = 5, noRetry: PartialFunction[Throwable, Boolean] = retryAll)(f: ⇒ Future[T])(implicit system: ActorSystem, ec: ExecutionContext): Future[T] = {
      retryImpl(1, maxAttempts, noRetry, expBackOffDelay, f)
    }

    def linear[T](maxAttempts: Int = 5, noRetry: PartialFunction[Throwable, Boolean] = retryAll, delay: FiniteDuration = 0 millis)(f: ⇒ Future[T])(implicit system: ActorSystem, ec: ExecutionContext): Future[T] = {
      retryImpl(1, maxAttempts, noRetry, _ ⇒ delay, f)
    }

    private def expBackOffDelay(n: Int) =
      (math.pow(2, math.min(n - 1, 7)).round * 100).millis

    private def retryImpl[T](attempt: Int, max: Int, noRetry: PartialFunction[Throwable, Boolean], delay: Int ⇒ FiniteDuration, f: ⇒ Future[T])(implicit system: ActorSystem, ec: ExecutionContext): Future[T] =
      if (attempt >= max) {
        f
      } else {
        val p = Promise[T]()
        f onComplete {
          case Success(res) ⇒ p.success(res)
          case Failure(e) ⇒
            if (noRetry.isDefinedAt(e) && noRetry(e)) {
              p.failure(e)
            } else {
              system.scheduler.scheduleOnce(delay(attempt)) {
                p.completeWith(retryImpl(attempt + 1, max, noRetry, delay, f))
              }
            }
        }
        p.future
      }

    private def retryAll = PartialFunction.empty[Throwable, Boolean]

    def unrecoverable: PartialFunction[Throwable, Boolean] = {
      case _: UnrecoverableError ⇒ true
    }
  }
}


class RichFuture[T](val future: Future[T]) extends AnyVal {

  def withTimeout(timeout: FiniteDuration)(implicit system: ActorSystem, ec: ExecutionContext): Future[T] = {
    val p = Promise[T]()

    system.scheduler.scheduleOnce(timeout) {
      p.tryFailure(new TimeoutException(s"Future timeout after $timeout"))
    }

    p.tryCompleteWith(future).future
  }
}
