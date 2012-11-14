package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.coursework.system.exceptions.UserError

class ItemNotFoundException(val item: Any) extends RuntimeException() with UserError