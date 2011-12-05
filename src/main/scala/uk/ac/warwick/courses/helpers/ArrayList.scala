package uk.ac.warwick.courses.helpers

/**
 * Allows you to create an empty Java ArrayList, useful as an initial
 * value for a variable that needs a mutable java.util.List.
 */
object ArrayList {
	def apply[T]() = new java.util.ArrayList[T]()
}