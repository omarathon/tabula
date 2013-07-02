package uk.ac.warwick.tabula

/**
 * PRS code is used in SITS to identify a member of staff.
 * It has the format DEPT CODE + UNIID e.g. IN0070790.
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
