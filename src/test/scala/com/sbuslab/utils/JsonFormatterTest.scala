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

  test("should deserialize boolean") {
    val o = JsonFormatter.deserialize[TestBoolean]("""{"isActive":true,"isTest":true}""")
    assert(o.isActive)
    assert(o.getIsTest)
  }

  test("should serialize boolean") {
    val o = JsonFormatter.serialize(new TestBoolean(true, true))
    assert(o == """{"isTest":true,"isActive":true}""")
  }
}


case class TestMe(
  name: String,
  num: BigDecimal = null
)
