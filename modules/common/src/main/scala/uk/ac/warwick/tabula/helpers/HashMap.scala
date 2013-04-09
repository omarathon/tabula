package uk.ac.warwick.tabula.helpers

import collection.JavaConverters._

/**
 * Allows you to create an empty Java HashMap, useful as an initial
 * value for a variable that needs a mutable java.util.Map.
 */
object HashMap {
	def apply[A, B](elements: (A, B)*): java.util.HashMap[A, B] = {
		val map = new java.util.HashMap[A, B]()
		elements foreach { case (key, value) => map.put(key, value) }
		map
	}
}