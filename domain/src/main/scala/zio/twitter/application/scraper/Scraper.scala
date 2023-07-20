package zio.twitter.application.scraper

import zio.*
import zio.http.Header.UserAgent
import zio.http.*
import zio.http.netty.NettyConfig
import zio.twitter.commons.Logger.Debug.reqAnnotation
import zio.twitter.commons.Logger.Debug.respAnnotation

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

    for
      _    <- ZIO.logDebug("Making a request") @@ reqAnnotation(req)
      resp <- Client.request(req).provide(createClientLayer(proxy, timeout))
      _    <- ZIO.logDebug("Got a response") @@ respAnnotation(resp)
    yield resp

  private def createClientLayer(proxy: Proxy, timeout: Duration) =
    val configWithProxy =
      if proxy == Proxy.empty then ZClient.Config.default
      else ZClient.Config.default.proxy(proxy)
    val clientLayer     =
      (ZLayer.succeed(
        configWithProxy
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
        .tapOutput(retry =>
          ZIO.logWarning(s"After $retry retry to make a request")
        )

    clientLayer.map(_.update[Client](_.retry(scheduler)))
