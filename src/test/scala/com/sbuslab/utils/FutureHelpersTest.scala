package com.sbuslab.utils

import java.util.concurrent.Executors
import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import org.assertj.core.api.Assertions._
import org.assertj.core.data.Offset
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

  test("should process future serial with fixed delay") {
    val start  = System.currentTimeMillis()

    val res = FutureHelpers.serialWithFixedDelay(List(1, 2, 3), delay = 100.millis) { n ⇒
      Future.successful("ok + " + n)
    }

    val re = Await.result(res, Duration.Inf)

    assertThat(re.length == 3)
    assertThat(System.currentTimeMillis() - start >= 300)
  }

  test("should run infinite schedule with fixed delay") {
    @volatile
    var counts = 0

    FutureHelpers.scheduleWithFixedDelay(0.millis, delay = 300.millis) { () ⇒
      Future {
        Thread.sleep(200)
        counts += 1
      }
    }

    val p = Promise[Any]()

    as.scheduler.scheduleOnce(1200.millis) {
      p.trySuccess((): Unit)
    }

    p.future map { _ ⇒
      assertThat(counts = 2)
    }
  }

  test("should run infinite schedule with atLeast delay - Future longer than delay") {
    @volatile
    var counts = 0

    FutureHelpers.scheduleWithAtLeastDelay(0.millis, delay = 200.millis) { () ⇒
      Future {
        Thread.sleep(400)
        counts += 1
      }
    }

    val p = Promise[Any]()

    as.scheduler.scheduleOnce(1300.millis) {
      p.trySuccess((): Unit)
    }

    p.future map { _ ⇒
      assertThat(counts = 3)
    }
  }

    test("should run infinite schedule with atLeast delay - Future shorter than delay") {
    @volatile
    var counts = 0

    FutureHelpers.scheduleWithAtLeastDelay(0.millis, delay = 400.millis) { () ⇒
      Future {
        Thread.sleep(200)
        counts += 1
      }
    }

    val p = Promise[Any]()

    as.scheduler.scheduleOnce(1300.millis) {
      p.trySuccess((): Unit)
    }

    p.future map { _ ⇒
      assertThat(counts = 3)
    }
  }
}
