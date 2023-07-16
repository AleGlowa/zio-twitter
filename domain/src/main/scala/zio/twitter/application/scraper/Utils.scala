package zio.twitter.application.scraper

import zio.Duration
import zio.NonEmptyChunk
import zio.durationInt
import zio.http.Header.AcceptLanguage.Multiple
import zio.http.Header.AcceptLanguage.Single
import zio.http.Header.Authorization
import zio.http.Header.Referer
import zio.http.Header.UserAgent
import zio.http.Header.UserAgent.*
import zio.http.Headers
import zio.http.URL

import java.time.LocalDate
import scala.util.Random

private[scraper] object TwitterUtils:
  private val ApiAuthorizationHeaderValue =
    "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs=1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA"

  def getHeaders(baseUrl: URL): Headers =
    Headers(
      Authorization.Bearer(ApiAuthorizationHeaderValue),
      Referer(baseUrl),
      Multiple(
        NonEmptyChunk(Single("en-US", Some(0.5)), Single("en", Some(0.5)))
      )
    )

  enum TwitterSearchScraperMode:
    case Live, Top, User

  enum TwitterApiType:
    case GraphQL

  enum ScrollDirection:
    case Top, Bottom, Both

  final case class GuestTokenManager(
    token: Option[String] = None,
    setTime: Duration = 0.seconds
  ):
    def reset: GuestTokenManager = copy(token = None, setTime = 0.seconds)
end TwitterUtils

def randUserAgent: UserAgent =
  val version = lerp(
    LocalDate.of(2023, 3, 7).toEpochDay + DaysSinceFirstYearTo1970,
    LocalDate.of(2030, 9, 24).toEpochDay + DaysSinceFirstYearTo1970,
    111,
    200,
    LocalDate.now.toEpochDay + DaysSinceFirstYearTo1970
  ) + Random.between(-5, 2) max 101

  Complete(
    Product("Mozilla", Some("5.0")),
    Some(
      Comment(
        s"(Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version.0.0.0 Safari/537.36"
      )
    )
  )

private val DaysSinceFirstYearTo1970 = 719163

private def lerp(a1: Long, b1: Long, a2: Long, b2: Long, n: Long) =
  (1.0 * (n - a1) / (b1 - a1) * (b2 - a2) + a2).toLong
