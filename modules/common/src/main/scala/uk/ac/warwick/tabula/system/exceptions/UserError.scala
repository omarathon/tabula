package uk.ac.warwick.tabula.system.exceptions

/**
 * Declares an exception caused by some user error, which
 * shouldn't be logged as an error in the logs.
 */
trait UserError {
	val statusCode: Int
}