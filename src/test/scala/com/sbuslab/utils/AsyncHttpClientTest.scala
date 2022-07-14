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

  // Start 2 http servers
  // proxyServer act as proxy
  val proxyServer = new WireMockServer(options.dynamicPort())
  // dummy http server refer to a regular server
  val dummyHttpServer = new WireMockServer(options.dynamicPort())

  //in this case all http calls goes through proxyServer. We should see proxyServer response instead of actual server.
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

  // in this case localhost is in non proxy hosts. So call goes directly to dummyHttpServer, proxy is ignored.
  // we should see response from dummy http server instead of proxy.
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
