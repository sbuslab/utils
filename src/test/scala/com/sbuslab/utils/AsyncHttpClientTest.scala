package com.sbuslab.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.sbuslab.utils.config.DefaultConfiguration
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}

@RunWith(classOf[JUnitRunner])
class AsyncHttpClientTest extends FunSuite with Logging with BeforeAndAfterAll {
  val ProxyResponse: String = "i_am_proxy_server"
  val DummyServerResponse: String = "i_am_dummy_server"

  val proxyServer = new WireMockServer(options.dynamicPort())
  val dummyHttpServer = new WireMockServer(options.dynamicPort())

  test("verify that request goes through proxy") {

    val config = ConfigFactory.parseString(
    s"""{
      |  sbuslab.http-client.proxy {
      |      host = "localhost"
      |      port = ${proxyServer.port()}
      |      nonproxy-hosts = ""
      |   }
      |}""".stripMargin
  )
    .withFallback(ConfigFactory.load("reference.conf"))

    val httpClient = DefaultConfiguration.getAsyncHttpClient(config)
    val response = httpClient.executeRequest(httpClient.prepareGet(s"http://localhost:${dummyHttpServer.port()}/").build()).get()
    assertResult(ProxyResponse) { response.getResponseBody }
  }

  test("verify that proxy is ignored") {

    val config = ConfigFactory.parseString(
    s"""{
      |  sbuslab.http-client.proxy {
      |      host = "localhost"
      |      port = ${proxyServer.port()}
      |      nonproxy-hosts = "localhost|127.0.0.1"
      |   }
      |}""".stripMargin
  )
    .withFallback(ConfigFactory.load("reference.conf"))

    val httpClient = DefaultConfiguration.getAsyncHttpClient(config)
    val response = httpClient.executeRequest(httpClient.prepareGet(s"http://localhost:${dummyHttpServer.port()}/").build()).get()
    assertResult(DummyServerResponse) { response.getResponseBody }
  }

  override def beforeAll(): Unit = {
    proxyServer.start()
    dummyHttpServer.start()

    proxyServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/")).willReturn(WireMock.aResponse.withBody(ProxyResponse)))
    dummyHttpServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/")).willReturn(WireMock.aResponse.withBody(DummyServerResponse)))

    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll
    proxyServer.stop()
    dummyHttpServer.stop()
  }
}
