package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.core.{ StringUtils => Utils }

/**
 * Scala-style String utilities. Adds the methods as implicit methods
 * on String.
 */
trait StringUtils {
	class SuperString(string: String) {
		def hasText = Utils hasText string
		def hasLength = Utils hasLength string
		def orEmpty: String = Option(string).getOrElse("")
		def maybeText: Option[String] = Option(string).filter(Utils.hasText)
		def textOrEmpty: String = maybeText.getOrElse("")
	}

	object HasText {
		def unapply(s: String): Option[String] = if (Utils hasText s) Some(s) else None
	}

	implicit def StringToSuperString(string: String) = new SuperString(string)
}

object StringUtils extends StringUtils