package com.sbuslab.utils

import scala.concurrent.{Future, Promise}

import net.spy.memcached.internal._


trait MemcacheSupport {

  implicit def asFutureGet[A](of: GetFuture[A]): Future[A] = {
    val promise = Promise[A]()

    of addListener { future ⇒
      try promise.success(future.get().asInstanceOf[A]) catch {
        case t: Throwable ⇒ promise.failure(t)
      }
    }

    promise.future
  }

  implicit def asFutureOperation[A](of: OperationFuture[A]): Future[A] = {
    val promise = Promise[A]()

    of addListener { future ⇒
      try promise.success(future.get().asInstanceOf[A]) catch {
        case t: Throwable ⇒ promise.failure(t)
      }
    }

    promise.future
  }

  implicit def asFutureBulk[A](of: BulkFuture[java.util.Map[String, A]]): Future[java.util.Map[String, A]] = {
    val promise = Promise[java.util.Map[String, A]]()

    of addListener { future ⇒
      try promise.success(future.get().asInstanceOf[java.util.Map[String, A]]) catch {
        case t: Throwable ⇒ promise.failure(t)
      }
    }

    promise.future
  }
}
