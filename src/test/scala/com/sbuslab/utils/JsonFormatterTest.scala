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
}
