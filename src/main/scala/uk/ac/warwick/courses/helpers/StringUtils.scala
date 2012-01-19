package uk.ac.warwick.courses.helpers

import uk.ac.warwick.util.core.{StringUtils => Utils}

/**
 * Scala-style String utilities. Adds the methods as implicit methods
 * on String.
 */
object StringUtils {
	class SuperString(string:String) {
		def hasText = Utils hasText string
		def hasLength = Utils hasLength string
		def orEmpty:String = Option(string).getOrElse("")
	}
	
	implicit def StringToSuperString(string:String) = new SuperString(string)
}