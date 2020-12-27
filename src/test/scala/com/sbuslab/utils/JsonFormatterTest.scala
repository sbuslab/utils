package com.sbuslab.utils

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class JsonFormatterTest extends FunSuite {

  test("should serialize scala BigDecimal to plain string") {

    val num = BigDecimal(BigDecimal(1000).bigDecimal.stripTrailingZeros())

    assert(num.toString == "1E+3")
    assert(JsonFormatter.serialize(num) == "\"1000\"")
  }

  test("should serialize null BigDecimal") {

    val o = TestMe("test1", null)

    assert(JsonFormatter.serialize(o) == """{"name":"test1"}""")
  }
}


case class TestMe(
  name: String,
  num: BigDecimal = null
)
