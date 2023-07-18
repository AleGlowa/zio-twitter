package zio.twitter.application.scraper

import zio.*
import zio.http.Header.Custom
import zio.http.Header.SetCookie
import zio.http.Header.Cookie as HCookie
import zio.http.*
import zio.json.DecoderOps
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import zio.twitter.application.scraper.TwitterUtils.*
import zio.twitter.domain.scraper.ValueClasses.NonEmptyString

import java.net.URI

import TwitterSearchScraper.*

final class TwitterSearchScraper(
  query: NonEmptyString,
  mode: TwitterSearchScraperMode = TwitterSearchScraperMode.Live,
  maxEmptyPages: Int = 20,
  override protected val connRetries: Int = 3
) extends Scraper:

  override protected val name: String = "twitter-search"

  private val url     = URL
    .fromURI(
      URI(
        "https",
        "twitter.com",
        "/search",
        s"f=live&lang=en&q=$query&src=spelling_expansion_revert_click",
        null
      )
    )
    .get
  private val headers = getHeaders(url)

  // Later: Change a dummy implementation
  override def getItems: List[String] = Nil
//    val paginationVars =

  private def iterApiData() = ???

  private def getApiData = ???

  // Later: Change from public to private
  def getHeadersWithGuestToken: RIO[ZState[State], Headers] =
    val getGuestTokenManager =
      for
        resp      <- get(url, headers)
        text      <- resp.body.asString
        gtManager <- retrieveGuestTokenManager(text, resp.headers.get(HCookie))
      yield gtManager
    for
      gtManager    <- ZIO.getStateWith[State](_.guestTokenManager)
      newGtManager <- getGuestTokenManager.when(gtManager.token.isEmpty)
    yield headers ++ newGtManager.fold(Headers.empty)(gtManager =>
      Headers(gtCookie(gtManager), gtHeader(gtManager.token.get))
    )

  private def retrieveGuestTokenManager(text: String, cookie: Option[HCookie]) =

    val fromText        = GuestTokenRegex.findFirstMatchIn(text).map(_.group(1))
    lazy val fromCookie =
      for
        c  <- cookie
        gt <- c.value.toCons.find(_.name == "gt")
      yield gt.content
    lazy val fromApi    =
      val url = URL
        .fromURI(
          URI("https", "api.twitter.com", "/1.1/guest/activate.json", null)
        )
        .get

      for
        resp <- post(url, Body.empty, headers)
        text <- resp.body.asString
        json <- ZIO.fromEither(text.fromJson[Json])
        gt   <- ZIO.fromEither(json.get(JsonCursor.field("guest_token").isString))
      yield gt.value

    for
      gt       <-
        (ZIO.getOrFail(fromText) <> ZIO.getOrFail(fromCookie) <> fromApi)
          .orDieWith(_ =>
            throw new NoSuchElementException("Unable to retrieve guest token")
          )
      gtManager = GuestTokenManager(token = Some(gt))
      _        <- ZIO.setState[State](State(gtManager))
    yield gtManager

object TwitterSearchScraper:
  // Later: Remove
  def getHeadersWithGuestToken   =
    ZIO.service[TwitterSearchScraper].flatMap(_.getHeadersWithGuestToken)
  private val GuestTokenRegex    =
    """document.cookie = decodeURIComponent\("gt=(\d+); Max-Age=10800; Domain=.twitter.com; Path=/; Secure"\)""".r
  private val GuestTokenValidity = 10800.seconds

  // Later: Change from public to private
  final case class State(guestTokenManager: GuestTokenManager)

  private def gtCookie(gtManager: GuestTokenManager) =
    SetCookie(
      Cookie.Response(
        "gt",
        gtManager.token.get,
        Some(".twitter.com"),
        Some(Path.root),
        isSecure = true,
        maxAge = Some(gtManager.setTime + GuestTokenValidity)
      )
    )
  private def gtHeader(token: String)                =
    Custom("x-guest-token", token)
