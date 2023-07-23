package zio.twitter.commons

import zio.IO
import zio.Task
import zio.ZIO

object ZIOUtils:
  extension [M <: String, A](io: IO[M, A])
    def orDieWithLog: Task[A] =
      io
        .tapError(msg => ZIO.logFatal(msg))
        .orDieWith(err => throw new RuntimeException(err))
