package uk.ac.warwick.tabula.helpers

import collection.JavaConverters._

/**
 * Allows you to create an empty Java ArrayList, useful as an initial
 * value for a variable that needs a mutable java.util.List.
 */
object ArrayList {
	def apply[T](elements: T*): java.util.ArrayList[T] = {
		val list = new java.util.ArrayList[T]()
		if (!elements.isEmpty) list.addAll(elements.asJavaCollection)
		list
	}
}