package zio.twitter.application.scraper

import zio.ZIO
import zio.ZLayer
import zio.duration2DurationOps
import zio.durationInt
import zio.http.Cookie
import zio.http.Header.Custom
import zio.http.Header.SetCookie
import zio.http.Path
import zio.test.ZIOSpecDefault
import zio.test.assertTrue
import zio.twitter.application.scraper.TwitterSearchScraper.State
import zio.twitter.application.scraper.TwitterUtils.GuestTokenManager
import zio.twitter.domain.scraper.ValueClasses.NonEmptyString

object TwitterSearchScraperSpec extends ZIOSpecDefault:
  private val layer =
    ZLayer.succeed(new TwitterSearchScraper(NonEmptyString("dummy")))

  private def expectedCookie(gtManager: GuestTokenManager) =
    SetCookie(
      Cookie.Response(
        "gt",
        gtManager.token.get,
        Some(".twitter.com"),
        Some(Path.root),
        isSecure = true,
        maxAge = Some(gtManager.setTime + 10800.seconds)
      )
    )
  private def expectedHeader(token: String)                =
    Custom("x-guest-token", token)

  def spec =
    suite("Getting guest token is correct")(
      test(
        "don't add the `gt` cookie and `x-guest-token` header when a token already exist"
      ) {
        val token             = Some("123")
        val guestTokenManager = GuestTokenManager(token)
        val state             = State(guestTokenManager)

        ZIO.stateful(state) {
          for
            headers   <- TwitterSearchScraper.getHeadersWithGuestToken
            gtManager <- ZIO.getStateWith[State](_.guestTokenManager)
          yield assertTrue(
            guestTokenManager == gtManager &&
              !headers.hasHeader(SetCookie) && !headers
                .hasHeader(Custom("x-guest-token", token.get))
          )
        }
      },
      test("add the `gt` cookie and `x-guest-token` header") {
        val state = State(GuestTokenManager(None))

        ZIO.stateful(state) {
          for
            headers   <- TwitterSearchScraper.getHeadersWithGuestToken
            gtManager <- ZIO.getStateWith[State](_.guestTokenManager)
            token      = gtManager.token.get
          yield assertTrue(
            headers.hasHeader(expectedCookie(gtManager)) && headers
              .hasHeader(expectedHeader(token))
          )
        }
      }
    ).provideShared(layer)
