package zio.twitter.application.scraper

import zio.*
import zio.http.Header.Custom
import zio.http.Header.SetCookie
import zio.http.Header.Cookie as HCookie
import zio.http.*
import zio.json.DecoderOps
import zio.json.EncoderOps
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import zio.twitter.application.scraper.TwitterUtils.*
import zio.twitter.commons.ZIOUtils.orDieWithLog
import zio.twitter.domain.scraper.ValueClasses.NonEmptyString

import java.net.URI

import TwitterSearchScraper.*

final class TwitterSearchScraper(
  query: NonEmptyString,
  mode: TwitterSearchScraperMode = TwitterSearchScraperMode.Live,
  maxEmptyPages: Int = 20,
  override protected val connRetries: Int = 3
) extends Scraper:
  self =>

  override protected val name: String = "twitter-search"

  private val url     =
    URL
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

  private def iterApiData(url: URL) = ???

  // Later: Change from public to private
  def getApiData(url: URL): RIO[ZState[State], Json] =
    for
      headersWithGuestToken <- self.getHeadersWithGuestToken
      resp                  <- self.get(graphQLUrl, headersWithGuestToken)
      text                  <- resp.body.asString
      json                  <- ZIO.fromEither(text.fromJson[Json]).orDieWithLog
    yield json

  // Later: Change from public to private
  def getHeadersWithGuestToken: RIO[ZState[State], Headers] =
    val getGuestTokenManager =
      for
        resp      <- self.get(url, headers)
        text      <- resp.body.asString
        gtManager <-
          self.retrieveGuestTokenManager(text, resp.headers.get(HCookie))
      yield gtManager
    for
      gtManager    <- ZIO.getStateWith[State](_.guestTokenManager)
      newGtManager <- getGuestTokenManager.when(gtManager.token.isEmpty)
    yield headers ++ newGtManager.fold(Headers.empty)(gtManager =>
      Headers(self.gtCookie(gtManager), self.gtHeader(gtManager.token.get))
    )

  private def retrieveGuestTokenManager(text: String, cookie: Option[HCookie]) =

    val fromText        =
      GuestTokenRegex.findFirstMatchIn(text).map(_.group(1))
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
        resp <- self.post(url, Body.empty, headers)
        text <- resp.body.asString
        json <- ZIO.fromEither(text.fromJson[Json]).orDieWithLog
        gt   <- ZIO
                  .fromEither(json.get(JsonCursor.field("guest_token").isString))
                  .orDieWithLog
      yield gt.value

    for
      _        <- ZIO.logInfo("Retrieving guest token")
      gt       <- ZIO.getOrFail(fromText) <> ZIO.getOrFail(fromCookie) <> fromApi
      gtManager = GuestTokenManager(token = Some(gt))
      _        <- ZIO.setState[State](State(gtManager))
      _        <- ZIO.logDebug(s"Using $gt guest token")
    yield gtManager

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

  private def gtHeader(token: String) =
    Custom("x-guest-token", token)

  private val variables =
    Json.Obj(
      "rawQuery"                 -> Json.Str(query),
      "count"                    -> Json.Num(20),
      "product"                  ->
        Json.Str(
          if self.mode == TwitterSearchScraperMode.Live then "Latest" else "Top"
        ),
      "withDownvotePerspective"  -> Json.Bool(false),
      "withReactionsMetadata"    -> Json.Bool(false),
      "withReactionsPerspective" -> Json.Bool(false)
    )

  private val graphQLUrl =
    URL
      .fromURI(
        URI(
          "https",
          "twitter.com",
          "/i/api/graphql/7jT5GT59P8IFjgxwqnEdQw/SearchTimeline",
          s"variables=${self.variables.toJson}&features=${features.toJson}",
          null
        )
      )
      .get

object TwitterSearchScraper:
  // Later: Remove
  val initstate                  = State(GuestTokenManager(None))
  // Later: Remove
  val layer                      = ZLayer.succeed(new TwitterSearchScraper(NonEmptyString("dummy")))
  // Later: Remove
  val url                        = URL
    .fromURI(
      URI(
        "https",
        "twitter.com",
        "/search",
        """variables={"rawQuery":"dummy","count":20}&features={"rweb_lists_timeline_redesign_enabled":false,"blue_business_profile_image_shape_enabled":false}""",
        null
      )
    )
    .get
  // Later: Remove
  def getHeadersWithGuestToken   =
    ZIO.service[TwitterSearchScraper].flatMap(_.getHeadersWithGuestToken)
  // Later: Remove
  def getApiData(url: URL)       =
    ZIO.service[TwitterSearchScraper].flatMap(_.getApiData(url))
  private val GuestTokenRegex    =
    """document.cookie = decodeURIComponent\("gt=(\d+); Max-Age=10800; Domain=.twitter.com; Path=/; Secure"\)""".r
  private val GuestTokenValidity = 10800.seconds

  // Later: Change from public to private
  final case class State(guestTokenManager: GuestTokenManager)

  private val features =
    Json.Obj(
      "rweb_lists_timeline_redesign_enabled"                                    -> Json.Bool(false),
      "blue_business_profile_image_shape_enabled"                               -> Json.Bool(false),
      "responsive_web_graphql_exclude_directive_enabled"                        -> Json.Bool(true),
      "verified_phone_label_enabled"                                            -> Json.Bool(false),
      "creator_subscriptions_tweet_preview_api_enabled"                         -> Json.Bool(false),
      "responsive_web_graphql_timeline_navigation_enabled"                      -> Json.Bool(true),
      "responsive_web_graphql_skip_user_profile_image_extensions_enabled"       ->
        Json.Bool(false),
      "tweetypie_unmention_optimization_enabled"                                -> Json.Bool(true),
      "vibe_api_enabled"                                                        -> Json.Bool(true),
      "responsive_web_edit_tweet_api_enabled"                                   -> Json.Bool(true),
      "graphql_is_translatable_rweb_tweet_is_translatable_enabled"              ->
        Json.Bool(true),
      "view_counts_everywhere_api_enabled"                                      -> Json.Bool(true),
      "longform_notetweets_consumption_enabled"                                 -> Json.Bool(true),
      "tweet_awards_web_tipping_enabled"                                        -> Json.Bool(false),
      "freedom_of_speech_not_reach_fetch_enabled"                               -> Json.Bool(false),
      "standardized_nudges_misinfo"                                             -> Json.Bool(true),
      "tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled" ->
        Json.Bool(false),
      "interactive_text_enabled"                                                -> Json.Bool(true),
      "responsive_web_text_conversations_enabled"                               -> Json.Bool(false),
      "longform_notetweets_rich_text_read_enabled"                              -> Json.Bool(false),
      "longform_notetweets_inline_media_enabled"                                -> Json.Bool(false),
      "responsive_web_enhance_cards_enabled"                                    -> Json.Bool(false),
      "responsive_web_twitter_blue_verified_badge_is_enabled"                   -> Json.Bool(true)
    )
