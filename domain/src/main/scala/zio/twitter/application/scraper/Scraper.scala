package zio.twitter.application.scraper

//import zio.http.*
//import zio.http.Header.UserAgent
//
//trait Scraper:
//
//  val name: String
//  val retries: Int = 3
//
//  def getItems: List[String]
//
//  private def request(
//    method: Method,
//    url: URL,
//    body: Body,
//    headers: Headers
//  ) = method match
//    case Method.GET =>
//      val tmp1 = Request.get(url).updateHeaders(_ ++ headers)
//      if ! headers.hasHeader(UserAgent) then
//        tmp1.addHeader(UserAgent)
