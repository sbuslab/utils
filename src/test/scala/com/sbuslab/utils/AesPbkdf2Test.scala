package com.sbuslab.utils

import org.apache.commons.lang.RandomStringUtils
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import com.sbuslab.utils.crypto.AesPbkdf2


@RunWith(classOf[JUnitRunner])
class AesPbkdf2Test extends FunSuite with Logging {

  test("encrypt and decrypt") {
    val pass = "my.test.pass" + RandomStringUtils.randomAlphanumeric(32)
    val data = "Secret string — 123! " + RandomStringUtils.randomAlphanumeric(2048)

    val encrypted = AesPbkdf2.encrypt(pass, data)
    val decrypted = AesPbkdf2.decrypt(pass, encrypted)

    assert(decrypted == data)
  }
}
