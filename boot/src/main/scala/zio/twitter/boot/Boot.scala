package zio.twitter.boot

import zio.*
import zio.logging.consoleLogger
import zio.twitter.application.scraper.TwitterSearchScraper
import zio.twitter.application.scraper.TwitterSearchScraper.url
import zio.twitter.commons.Logger.loggerConfig

object Boot extends ZIOAppDefault:

  override val bootstrap: Layer[Config.Error, Unit] =
    Runtime.removeDefaultLoggers >>> Runtime.setConfigProvider(
      loggerConfig
    ) >>> consoleLogger()

  override val run: UIO[ExitCode] =
    (for
      _ <- TwitterSearchScraper.getApiData(url) //ZIO.logDebug("Hello world!")
      //_ <- ZIO.logDebug("I was compiled by Scala 3. :)")
    yield ()).exitCode.provide(TwitterSearchScraper.layer, ZState.initial(TwitterSearchScraper.initstate))
