package com.sbuslab.utils

import java.util.concurrent.Executors
import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import org.assertj.core.api.Assertions._
import org.assertj.core.data.Offset
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.springframework.beans.factory.annotation.Autowired

@RunWith(classOf[JUnitRunner])
class FutureHelpersTest extends FunSuite {

  @Autowired
  implicit private val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100))
  implicit private val as: ActorSystem = ActorSystem("default")

  test("should process tasks in serial batches in parallel") {

    val start = System.currentTimeMillis()
    val resultFuture: Future[Seq[Int]] = FutureHelpers.serialInParallelBatch(Seq.range(0, 100))(20)(
      elem ⇒ Future {
        Thread.sleep(500)
        elem * elem
      }
    )
    val ints: Seq[Int] = Await.result(resultFuture, Duration.Inf)
    val finish = System.currentTimeMillis()
    assertThat(ints.toList.asJava.size()).isEqualTo(100)
    assertThat(finish - start).isCloseTo(2500L, Offset.offset[java.lang.Long](300L))
  }

  test("test") {
    val start  = System.currentTimeMillis()

    val res = FutureHelpers.serialWithFixedDelay(List(1, 2, 3), delay = 100.millis) { n ⇒
      Future.successful("ok + " + n)
    }

    val re = Await.result(res, Duration.Inf)

    assertThat(re.length == 3)
    assertThat(System.currentTimeMillis() - start >= 300)
  }
}
