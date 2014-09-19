package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.core.{ StringUtils => Utils }
import language.implicitConversions

/**
 * Scala-style String utilities. Adds the methods as implicit methods
 * on String.
 */
trait StringUtils {
	class SuperString(string: String) {
		def hasText = Utils hasText string
		def isEmptyOrWhitespace = !hasText
		def hasLength = Utils hasLength string
		def safeTrim = Option(string).map { _.trim }.getOrElse("")
		def safeSubstring(proposedStart: Int) = Utils safeSubstring(string, proposedStart)
		def safeSubstring(proposedStart: Int, proposedEnd: Int) = Utils safeSubstring(string, proposedStart, proposedEnd)
		def orEmpty: String = Option(string).getOrElse("")
		def maybeText: Option[String] = Option(string).filter(Utils.hasText)
		def textOrEmpty: String = maybeText.getOrElse("")
		def safeLowercase = Option(string).map { _.toLowerCase }.getOrElse("")
		def safeLength = Option(string).fold(0) { _.length }
		def safeContains(substring: String) = Option(string).exists(_.contains(substring))
	}

	implicit def StringToSuperString(string: String) = new SuperString(string)
	
	implicit class Regex(sc: StringContext) {
		def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
	}
}

object StringUtils extends StringUtils