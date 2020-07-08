package com.sbuslab.utils

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Failure


trait Memoize {

  case class CachedObject(expiredAt: Long, obj: Any)

  private val cache = new ConcurrentHashMap[String, CachedObject]()
  private val disabledCache = sys.env.getOrElse("DISABLED_MEMOIZE_CACHE", "false") == "true"

  def memoize[T](key: String, timeout: Duration)(f: ⇒ T)(implicit e: ExecutionContext): T =
    cache.compute(key, (_, exist) ⇒ {
      if (exist == null || disabledCache || exist.expiredAt < System.currentTimeMillis()) {
        val result = f

        result match {
          case f: Future[_] ⇒ f andThen { case _: Failure[_] ⇒ cache.remove(key) }
          case _ ⇒
        }

        CachedObject(System.currentTimeMillis() + timeout.toMillis, result)
      } else {
        exist
      }
    }).obj.asInstanceOf[T]

  def memoizeClear(key: String): Unit =
    cache.remove(key)
}
