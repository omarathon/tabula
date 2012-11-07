package uk.ac.warwick.courses

import uk.ac.warwick.courses.system.exceptions.UserError

class ItemNotFoundException(val item: Any) extends RuntimeException() with UserError