package zio.twitter.application.scraper

import zio.*
import zio.http.Header.UserAgent
import zio.http.*
import zio.http.netty.NettyConfig

trait Scraper:

  protected val name: String
  protected val connRetries: Int = 3

  def getItems: List[String]

  protected def get(url: URL, headers: Headers): Task[Response] =
    request(Method.GET, url, Body.empty, headers)

  protected def post(url: URL, body: Body, headers: Headers): Task[Response] =
    request(Method.POST, url, body, headers)

  private def request(
    method: Method,
    url: URL,
    body: Body,
    headers: Headers,
    proxy: Proxy = Proxy.empty,
    timeout: Duration = 10.seconds
  ) =
    val headersWithUserAgent =
      if !headers.hasHeader(UserAgent) then headers.addHeader(randUserAgent)
      else headers
    val req                  =
      Request.default(method, url, body).addHeaders(headersWithUserAgent)

    Client.request(req).provide(createClientLayer(proxy, timeout))

  private def createClientLayer(proxy: Proxy, timeout: Duration) =
    val clientLayer =
      (ZLayer.succeed(
        ZClient.Config.default
          .proxy(proxy)
          .connectionTimeout(timeout)
          .addUserAgentHeader(false)
      ) ++ ZLayer.succeed(
        NettyConfig.default
      ) ++ DnsResolver.default) >>> ZClient.live

    val scheduler =
      Schedule
        .exponential(1.second)
        .repetitions
        .whileOutput(_ <= connRetries)

    clientLayer.map(_.update[Client](_.retry(scheduler)))
