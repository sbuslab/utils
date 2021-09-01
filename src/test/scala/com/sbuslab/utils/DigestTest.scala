package com.sbuslab.utils

import java.security.{KeyPairGenerator, MessageDigest, SecureRandom, Security}

import net.i2p.crypto.eddsa.{EdDSAEngine, EdDSAPrivateKey, EdDSAPublicKey, EdDSASecurityProvider}
import net.i2p.crypto.eddsa.spec.{EdDSANamedCurveTable, EdDSAPrivateKeySpec, EdDSAPublicKeySpec}
import org.apache.commons.lang.RandomStringUtils
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class DigestTest extends FunSuite with Logging {

  test("encrypt and decrypt rsa") {
    val pass = "my.test.pass" + RandomStringUtils.randomAlphanumeric(32)
    val data = "Secret string — 123! " + RandomStringUtils.randomAlphanumeric(32)

    val rsa       = Digest.generateRsaKeyPair()
    val encrypted = Digest.encryptRsa(rsa.getPublicKey, data)
    val decrypted = Digest.decryptRsa(rsa.getPrivateKey, encrypted)

    assert(decrypted == data)
  }

  test("backward compatibility with old rsa-encryption") {
    val encrypted = "OJmgk9bt1kOANRZ5HRfbkpAtsLTHyO/XsQwfnPaLkTMNRQIHtcBVE3GN1Qa6atmBIo/r+D2W/RDJHSHLMILck2zO+pfkXraNbDpSx3UeY0cVzcZG6B5w+9DN+ws1V9tA6y68bPrzpxK1cg5ltmdf/Xob9HXH1vHyh4ySoiziKPV9emnYpjVLQh1esUc87A4j3h7jRus33V9dYcKcqfglbUof3+BdMO20baeOrL5ZZTzb/DC/pdu/Eg4HxH5Ea8V4xkidMCMKTHmv/aq4IV2DW/R1cG+7i+96Q9D0ZWYiPfuk/vOgy4IPkq1rOFeyBJ1rdHD1HVP0VsuwCWsj/96drg=="
    val decrypted = Digest.decryptRsa("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCgjBFTvd+OadMIMbvSXPrUkSbuPlT0/kQk9cCAO2z4pxLabmpVczyrkdCU5NpGy4JiJmhR0vGYnSvtYjLGGA1n+KfFDD8veIEL8MJzpCipyfIytyIaZ45QB5r7p6d3vf1HwppMYDWEqYw3W/lfWEYbiz4tj/tiAZRsMdhAR3knAR8FCcEgqW8RYKIuUzroCW003+6jhbH6ggGtaiyxhpEuZljOzPK29e1ynCkDOzLnYtCQBdzywnQbL6VLXiDXNwQGeHB+wQFxvXX/NEH8RGI0+MoXWG5mEJBvPCgOdPhoWJUWT1TUAlqSvzVXF8+OsOhG9c5AKOHZaYuxRUKkTU9RAgMBAAECggEAEDl3es4CbKL3J6nXAFBTLO4qUANOlJ+phU6skIjw30QLHLXx2wbVR8VpndJu2J6yY3bcLgCyV6jyin/U/73ohOxVEA3HyOVVrT48eHFLhrUY29U0BhUXBbFvESWFQA2cLgdVjohegDSeLe4GioMiOqBcvUACuInOVQzIfN58ONu0CFVH4o1yh5NNRdC71GK1VtVu+kTvf0V4NIxA9kNUsmN3KizkxlKqkPU0q2ipmZAQPUHN/rGWtXx2w0UP45QWZUQDfoEO0phl2m2OsRtGmTGJk9K8q6gXjowGyw7jRzU8Ut27lGJV6fDuF1UpIefzD9HkeUvxQ8fR63Xw+M0izQKBgQDcKaTroyEIeB6GYzvWw61TDlT0cApuFnY3PTp48PCzGyYoz4l0+gkOY9vHfDGeZlxt7FUqYra/LhuaX1Do6nmg/P2GVTPa9IKkXGOdSu49DaMwZQOmYYZ6vJ4rVhyqtFwWS8U0x8d7cL+3nuT69OCDttyKp0CZ1qEvUhueQa9zqwKBgQC6rjI6f2AVFAb5npAKnd1Ukr3t9aPIYsqGmen9ox7RNuvdD7BhDIil3Y4HwEUbDAjnyYetP3hOfgViLC8cdMzFlsfEOTkk9NpSvo1X8MSC6Hqjk/AINJgvVrdKuAxMYpn8hqmSVzhsyTe6754TN2ZeT1s70hkUHEB3xBdrdYOM8wKBgG3rHLvIpiVkU1klEisXZuGgtimUBqEP9dV+bEMViBbj5xlZBHeynfLhSElAaEV9NYhsBdkzj6nDi2R3Uh33cuI9bRY0U79tdAw7VECjuG4i8OaoiDn5VvrQUOeyBn3zrkYzbjH7zEyE7jLu0cO5np4kHdfbyRUFY1QM2l0YzlAhAoGBALNfC34vw6edLbKNy8OXugI4WWya+PtjCUxZ+X4vTT4jcnBfUc9+VpJhFsaV1RDO+IOWndo2wSdSaWPSJpZGGDfG0D3X6fN7nYeh9nfILVnHfYdNb3bC6nOZ89ZEj/SZTXJaBfdJ/TpyAS2Kba3zLlZyE6ygDF7JYpxxeztSl8tHAoGBAJOAQdehlzCIO0bP5PFMVAdjJkIEJJDXs5k5a5T5ag4dtj6KFElPttROiiGPip6vpl7hkg07zghAfUCL3AvDRcJCY8IJE0cIQiekpvJp7WtaAQClccCCTnRPhfkiHts20t73d9dlAHHDVovueHwUMxf9s2UEBypX4XgVu0Pw/wEA", encrypted)

    assert(decrypted == "Secret string — 123! ")
  }

  test("decrypt rsa from ts digest") {
    val privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNkAXSaxT4LVeakJLgY1WWnRFWSbr7o96lw7DL6vc1lSkqV/FpAh7QF5nVeBkIa039pQV7ct/JmIBqNmivXP2qfRuFQCKxMqwe/JQQZ9aqvJ2Rxpyn98plD2iVxSGNmjFwO7uXjmQGOQS35VXhJeVz7C6UxAOe4c1RUjMBtRV+lrhfglanCBmrkuM8WtD8tYOZmxvk8M2PJiLENaIulS8+RlaQB+1QZqMQD/XdYz97YqLd56KcDAaK425NbzAnU0A1gZlpUnRwSTPRSpqhiU5E3qC+BLPUkS0KPSSU8kFHIDJURTDNEhoYO1Y7mCym3kDpVW6VBoefL+kw9Q2eXGdvAgMBAAECggEAPR0AXhpv1YjFbrJkuElP/MMdkGXDLWT+oJGZUka80DVUU75piyqSNpONrKVT6+ja+vnAs37ngWvRYcJjRR+EWtdvAyKaLcw+18eHzYjRjVkeD2TY1icZCQqXsU82Nn8NC6z2jIxMuHzjJjkMWy+FE/23q4rK+MVw4Shq+ar4hPWJZ0ydfo4miF9bdIBPfFU2PPSquqxqCbkUrYKnSjkM1hDQ354P30dPlrslKCHz6uC4mJdMRWcA9Zp4oImsE/Sg1YqgHs50XgPhnHAq7Y74OPC1fmsNzeDVOWiS8LBfqKZ87FlTShIGEJoUy6K5+/yJCDO3d2ZsLteVlMJf2uYjAQKBgQD48kXCXPNBsVoeeEUwg9k3jQfcZBChr/gzlqPLH1eH5oaPbZZIRHYwXl1xxDHBHtOEmfOPFAxBctJfq9XhC41VRLR1nMCVGDuaMvhEdG5YMqHn2BL2mfsG0wN8Q6lYuCkvC+MPOn4vPCEQGsr/WyVHinnSkxIM7urqu3Aom7b/zwKBgQDTYxEa5WOO5DqBVqqEm+f7txTAAukLQk+/r5oUsjZcW2gF/FrXlnzJitKTnKW/JvGP+rEb020pDCTXaHC6TR0mdT/Y/kD/7CcKxGa8lcWWQ0/s/hziepCkh9/+En1NWOi/3man3GbSmNRARZjPV7mJ8TdgXkVeSq6SxHVvX5dmYQKBgQDbdkF3QhgU7rOCrwZX6bQhm8u1R+W9lHLbj85fsAarQNeZM8PLe8cxhs8cDxjJplJT0KB9nu/a9s1tGABZ/6Qd6o6oLIM+LPnGS27AcmAgkqpWyA6XpVE+R+IHt2JgWyG7XuBRuYAqRfjjyKryiLiJpBMXRx4flrQ0MP+EXRnr0wKBgGnyWi1f0TFaBFS06kwClBfeIAFItCLXfn71dUVwOLy7d3ygiatKGostD/O57HzM+P+Px1rJ8glDs+deyjkl0zlmRLTuYgejBcJow5E7g8eXPyTqC/IbOgmsrEB8Zd7xc95OiqcRWVuuC5uXBkrNjgmsI3Qia6QhzDN1UeKbR5ehAoGBAOm32FCP+uidDx8abAC1YZQ8PNihktQc+LC5Tx96Fgcbiq0lq3DrsaZuIPbFfeWHQ3lSCL7tPGbfcI1DoQ3lDouqrwV4LA6lKjV4x0KlPAepyT2tMy8cy9GhRi2lSQthGwm64WtoH+1ghhNiIb3M2vUuczFiTMb25SBIYdS2loOs"
    val encrypted  = "BixEsK19DeL1vlZe8XYGJWiRTGiO+f4F0+rmeCWP9pFqt8FgLdHqu0NL8LtMdHL8qVa0PcmJDwvDkMnPz4Z9NZ29jIHBnkBHhT3C0BdJx2OBwONX/vp8AwL8x56/nAQI9imffQ0ATE8s0oZ2HSAyH3oy+ENA3XuUxFLQRWsdsyGmKJIIdYmSaCfxd78hvuikN6MtqKhKdpmj0Kiqe1JtYRNoTOgc0kKG3WKOefiVWtpyuBmkrmXJALNZubtr1vQfd7pHGSz/gLJ8iheXYNLzTvcVhiKnILawfJD3RlWmscCUUXjhyd3N8KCvCiCHHWv79D6W1OPfxWgIafWrcEGzRQ==\nAYt093BWGYZXREX4XWpg4TBMbwcmzvvfcxNjujC4PIOeM2WWbRdZgjvnR/Tm9GX/L7Lf9zFnGnXPAg=="

    val decrypted = Digest.decryptRsa(privateKey, encrypted)
    assert(decrypted == "Hello from TS")
  }

  test("sign and verify Ed25519 signature") {

    val spec = EdDSANamedCurveTable.getByName("Ed25519")

    val keyPairGenerator = KeyPairGenerator.getInstance("EdDSA", "EdDSA")
    keyPairGenerator.initialize(EdDSANamedCurveTable.getByName("Ed25519"), SecureRandom.getInstanceStrong)

    val pair = keyPairGenerator.generateKeyPair
    val keys = new Digest.GeneratedKeys(Digest.hex(pair.getPublic.asInstanceOf[EdDSAPublicKey].getAbyte), Digest.hex(pair.getPrivate.asInstanceOf[EdDSAPrivateKey].getSeed))

    println(s"\n | pub: ${keys.getPublicKey} → priv: ${keys.getPrivateKey} \n |\n")

    // sign

    val sgr     = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm))
    val privKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(Digest.unhex(keys.getPrivateKey), spec))

    sgr.initSign(privKey)

    sgr.update("test me".getBytes)
    val signature = sgr.sign()

    // verify

    val vrf    = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm))
    val pubKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(Digest.unhex(keys.getPublicKey), spec))

    vrf.initVerify(pubKey)

    vrf.update("test me".getBytes)

    assert(vrf.verify(signature))
  }
}
