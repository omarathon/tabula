package uk.ac.warwick.courses.system.exceptions

object ExceptionTokens {
	private var counter = System.currentTimeMillis()
	
	def newToken = {
		counter += 1
		counter.toString
	}
}