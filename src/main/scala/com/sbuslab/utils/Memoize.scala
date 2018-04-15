package com.sbuslab.utils

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Failure


trait Memoize {

  case class CachedObject(expiredAt: Long, obj: Future[Any])

  val cache = new ConcurrentHashMap[String, CachedObject]()

  def memoize[T](key: String, timeout: Duration)(f: ⇒ Future[T])(implicit e: ExecutionContext): Future[T] =
    cache.compute(key, (_, exist) ⇒ {
      if (exist == null || exist.expiredAt < System.currentTimeMillis()) {
        CachedObject(System.currentTimeMillis() + timeout.toMillis, f andThen {
          case _: Failure[_] ⇒ cache.remove(key)
        })
      } else {
        exist
      }
    }).obj.asInstanceOf[Future[T]]
}
