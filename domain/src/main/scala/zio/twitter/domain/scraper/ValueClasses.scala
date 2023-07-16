package zio.twitter.domain.scraper

import zio.prelude.Assertion
import zio.prelude.Assertion.isEmptyString
import zio.prelude.Subtype

object ValueClasses:

  object NonEmptyString extends Subtype[String]:
    override inline def assertion: Assertion[String] =
      !isEmptyString
  type NonEmptyString = NonEmptyString.Type
