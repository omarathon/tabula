package uk.ac.warwick.tabula.helpers

import collection.JavaConverters._

/**
 * Allows you to create an empty Java ArrayList, useful as an initial
 * value for a variable that needs a mutable java.util.List.
 */
object ArrayList {
	def apply[A](elements: A*): java.util.ArrayList[A] = {
		val list = new java.util.ArrayList[A]()
		if (!elements.isEmpty) list.addAll(elements.asJavaCollection)
		list
	}
}