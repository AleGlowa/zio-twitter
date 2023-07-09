package zio.twitter

object Boot:
  @main def hello(): Unit =
    println("Hello world!")
    println("I was compiled by Scala 3. :)")
