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
		def hasLength = Utils hasLength string
		def safeSubstring(proposedStart: Int) = Utils safeSubstring(string, proposedStart)
		def safeSubstring(proposedStart: Int, proposedEnd: Int) = Utils safeSubstring(string, proposedStart, proposedEnd)
		def orEmpty: String = Option(string).getOrElse("")
		def maybeText: Option[String] = Option(string).filter(Utils.hasText)
		def textOrEmpty: String = maybeText.getOrElse("")
	}

	implicit def StringToSuperString(string: String) = new SuperString(string)
}

object StringUtils extends StringUtils