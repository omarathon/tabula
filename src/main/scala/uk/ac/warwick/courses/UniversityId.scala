package uk.ac.warwick.courses
import scala.util.matching.Regex

object UniversityId {
	val universityIdPattern = new Regex("^[0-9]{7}$")
	
	def isValid(id:String) = id match {
		case universityIdPattern() => true
		case _ => false
	}
}