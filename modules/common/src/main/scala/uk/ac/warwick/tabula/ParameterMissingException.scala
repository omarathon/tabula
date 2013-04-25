package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.system.exceptions.UserError
import org.apache.http.HttpStatus
import org.springframework.web.bind.MissingServletRequestParameterException

class ParameterMissingException(ex: MissingServletRequestParameterException) extends RuntimeException(ex) with UserError {
	override val statusCode = HttpStatus.SC_BAD_REQUEST
}