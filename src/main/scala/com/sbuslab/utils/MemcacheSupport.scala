package com.sbuslab.utils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

import net.spy.memcached.internal._
import net.spy.memcached.MemcachedClient


trait MemcacheSupport {

  protected val disabledMemoizeMemcached = sys.env.getOrElse("DISABLED_MEMOIZE_CACHE", "false") == "true"

  protected def memcached[T: Manifest](key: String, timeout: Duration)(f: ⇒ Future[T])(implicit e: ExecutionContext, memClient: MemcachedClient): Future[T] =
    if (disabledMemoizeMemcached) f else {
      memClient.asyncGet("memcached:" + key) flatMap { result ⇒
        if (result != null) {
          Future.fromTry(Try(JsonFormatter.deserialize[T](result.toString)))
        } else {
          f andThen {
            case Success(result) ⇒
              memClient.set("memcached:" + key, timeout.toSeconds.toInt, JsonFormatter.serialize(result))
          }
        }
      }
    }

  protected def memcachedClear(key: String)(implicit memClient: MemcachedClient) =
    if (!disabledMemoizeMemcached) {
      memClient.delete("memcached:" + key)
    }

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
