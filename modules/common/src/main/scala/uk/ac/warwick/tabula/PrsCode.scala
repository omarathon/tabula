package uk.ac.warwick.tabula

/**
 * SCJ (student course join) code is used in SITS to identify a student on a particular course of theirs.
 * it has the format UNIID/COURSENUM. Coursenum starts at 1 and is incremented whenever the student
 * starts a new course.
 */
object PrsCode {
	def getUniversityId(prsCode: String) = {
		if (prsCode == null || prsCode.length() != 9) {
			None
		}
		else {
			val uniId = prsCode.substring(2)
			if (uniId forall Character.isDigit ) Some(uniId)
			else None
		}
	}
}
