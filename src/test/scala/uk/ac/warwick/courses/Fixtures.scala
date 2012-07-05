package uk.ac.warwick.courses

import uk.ac.warwick.courses.data.model.Submission

object Fixtures {
    
	def submission() = {
		val s = new Submission
		s.universityId = "0123456"
		s.userId = "cuspxp"
		s
	}
	
}