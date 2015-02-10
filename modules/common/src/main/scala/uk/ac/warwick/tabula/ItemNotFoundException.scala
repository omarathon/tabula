package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.system.exceptions.UserError
import org.apache.http.HttpStatus

class ItemNotFoundException(val item: Any) extends RuntimeException() with UserError {
	def this() {
		this(())
	}

	override val statusCode = HttpStatus.SC_NOT_FOUND
}