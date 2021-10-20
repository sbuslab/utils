package com.sbuslab.utils

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

import net.spy.memcached.MemcachedClient


trait Memoize {

  case class CachedObject(expiredAt: Long, obj: Any)

  private val memoizeCache = new ConcurrentHashMap[String, CachedObject]()
  private val disabledMemoizeCache = sys.env.getOrElse("DISABLED_MEMOIZE_CACHE", "false") == "true"

  private val fallbackMaxTtl = 1.day

  def memoize[T](key: String, timeout: Duration)(f: ⇒ T)(implicit ec: ExecutionContext): T =
    memoizeCache.compute(key, (_, exist) ⇒ {
      if (exist == null || disabledMemoizeCache || exist.expiredAt < System.currentTimeMillis()) {
        val result = f

        result match {
          case f: Future[_] ⇒ f andThen { case _: Failure[_] ⇒ memoizeCache.remove(key) }
          case _ ⇒
        }

        CachedObject(System.currentTimeMillis() + timeout.toMillis, result)
      } else {
        exist
      }
    }).obj.asInstanceOf[T]

  def memoizeFallback[T](key: String)(f: ⇒ Future[T])(implicit ec: ExecutionContext): Future[T] =
    if (disabledMemoizeCache) f else {
      (try f catch {
        case NonFatal(e) ⇒ Future.failed(e)
      }) andThen {
        case Success(result) ⇒ memoizeCache.put("fallback:" + key, CachedObject(System.currentTimeMillis() + fallbackMaxTtl.toMillis, result))
      } recover {
        case NonFatal(e) ⇒
          memoizeCache.get("fallback:" + key) match {
            case null ⇒ throw e
            case result ⇒ result.obj.asInstanceOf[T]
          }
      }
    }

  def memoizeWithFallback[T: Manifest](key: String, timeout: Duration)(f: ⇒ Future[T])(implicit ec: ExecutionContext): Future[T] =
    memoize(key, timeout) {
      memoizeFallback(key)(f)
    }

  def memoizeClear(key: String): Unit =
    memoizeCache.remove(key)
}
