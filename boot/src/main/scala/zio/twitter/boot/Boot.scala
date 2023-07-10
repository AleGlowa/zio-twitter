package zio.twitter.boot

import zio.*

object Boot extends ZIOAppDefault:

  override val run: UIO[ExitCode] =
    (for
      _ <- Console.printLine("Hello world!")
      _ <- Console.printLine("I was compiled by Scala 3. :)")
    yield ()).exitCode
