package uk.ac.warwick.tabula.coursework
import scala.util.matching.Regex

object UniversityId {

	/**
	 * A regex for the Warwick University ID format, which is
	 * always a 7 digit character.
	 */
	val universityIdPattern = new Regex("^[0-9]{7}$")

	/** Returns whether this string matches universityIdPattern. */
	def isValid(id: String) = id match {
		case universityIdPattern() => true
		case _ => false
	}
}