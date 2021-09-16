package com.sbuslab.utils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}
import scala.util.control.NonFatal

import net.spy.memcached.internal._
import net.spy.memcached.MemcachedClient


trait MemcacheSupport {

  protected val disabledMemoizeMemcached = sys.env.getOrElse("DISABLED_MEMOIZE_CACHE", "false") == "true"

  protected def memcached[T: Manifest](key: String, timeout: Duration)(f: ⇒ Future[T])(implicit ec: ExecutionContext, memClient: MemcachedClient): Future[T] =
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

  protected def memcachedLazy[T: Manifest](key: String, timeout: Duration)(f: ⇒ Future[T])(implicit ec: ExecutionContext, memClient: MemcachedClient): Future[T] =
    if (disabledMemoizeMemcached) f else {
      memClient.asyncGet("memcached:" + key) flatMap { result ⇒
        if (result != null) {
          Future.fromTry(Try(JsonFormatter.deserialize[CachedObject[T]](result.toString))) map { cachedObject ⇒
            if (cachedObject.exp < System.currentTimeMillis()) {
              renewLazyCache(key, timeout, f)  // update cache asynchronously
            }

            cachedObject.obj
          }
        } else {
          renewLazyCache(key, timeout, f)
        }
      }
    }

  private def renewLazyCache[T: Manifest](key: String, timeout: Duration, f: ⇒ Future[T])(implicit ec: ExecutionContext, memClient: MemcachedClient): Future[T] =
    f andThen {
      case Success(result) ⇒
        memClient.set("memcached:" + key, timeout.toSeconds.toInt * 2, JsonFormatter.serialize(CachedObject(result, System.currentTimeMillis() + timeout.toMillis)))
    }

  protected def memcachedDeduplicate[T](key: String)(f: ⇒ T)(implicit ec: ExecutionContext, memClient: MemcachedClient): Option[T] =
    if (disabledMemoizeMemcached) Some(f) else {
      if (memClient.get("dedup:" + key) == null) {
        memClient.set("dedup:" + key, 0, 1)
        Some(f)
      } else None
    }

  protected def memcachedFallback[T: Manifest](key: String)(f: ⇒ Future[T])(implicit ec: ExecutionContext, memClient: MemcachedClient): Future[T] =
    if (disabledMemoizeMemcached) f else {
      (try f catch {
        case NonFatal(e) ⇒ Future.failed(e)
      }) andThen {
        case Success(result) ⇒ memClient.set("fallback:" + key, 0, JsonFormatter.serialize(result))
      } recoverWith {
        case NonFatal(e) ⇒
          memClient.asyncGet("fallback:" + key) flatMap {
            case null ⇒ throw e
            case result ⇒ Future.fromTry(Try(JsonFormatter.deserialize[T](result.toString)))
          }
      }
    }

  protected def memcachedWithFallback[T: Manifest](key: String, timeout: Duration)(f: ⇒ Future[T])(implicit ec: ExecutionContext, memClient: MemcachedClient): Future[T] =
    memcached(key, timeout) {
      memcachedFallback(key)(f)
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


case class CachedObject[T](obj: T, exp: Long)
