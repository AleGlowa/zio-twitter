package zio.twitter.application.scraper

import zio.http.Header.UserAgent
import zio.test.*
import zio.twitter.application.scraper.Utils.randUserAgent

import scala.util.Random

object UtilsSpec extends ZIOSpecDefault:

  def spec =
    suite("randUserAgent is correct")(
      test("version in User-Agent header is correct") {
        Random.setSeed(2023)
        val versionRegex = """(\d+).0.0.0""".r

        val rawUserAgent = UserAgent.render(randUserAgent)
        versionRegex.findFirstMatchIn(rawUserAgent) match
          case Some(version) =>
            assertTrue(version.group(1) == "112")
          case None          =>
            throw new Exception("Version doesn't exist in User-Agent header")
      }
    )
