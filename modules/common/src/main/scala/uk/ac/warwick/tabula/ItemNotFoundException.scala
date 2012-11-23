package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.system.exceptions.UserError

class ItemNotFoundException(val item: Any) extends RuntimeException() with UserError