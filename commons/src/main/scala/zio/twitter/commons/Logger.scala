package zio.twitter.commons

import zio.ConfigProvider
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Request
import zio.http.Response
import zio.logging.LogAnnotation

object Logger:
  val loggerConfig: ConfigProvider =
    TypesafeConfigProvider.fromResourcePath()

  object Info:
    val reqAnnotation: LogAnnotation[Request] =
      LogAnnotation[Request]("request", (_, req) => req, _.url.encode)

  object Debug:
    val reqAnnotation: LogAnnotation[Request] =
      LogAnnotation[Request]("request", (_, req) => req, _.toString)

    val respAnnotation: LogAnnotation[Response] =
      LogAnnotation[Response](
        "response",
        (_, resp) => resp,
        r => s"Response(${r.status},${r.body},${r.headers})"
      )
