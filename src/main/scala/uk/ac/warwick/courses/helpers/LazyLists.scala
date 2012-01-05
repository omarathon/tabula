package uk.ac.warwick.courses.helpers

import org.apache.commons.collections.list.LazyList
import org.apache.commons.collections.FactoryUtils
import java.util.{List=>JList}
import uk.ac.warwick.courses.commands.assignments.FeedbackItem

object LazyLists {
	
	/**
	 * Creates a lazy list (optionally based on an existing list) that will create
	 * elements on demand using the default no-arg constructor for the class.
	 */
	def simpleFactory[T] (list:JList[T] = ArrayList[T]()) (implicit m:Manifest[T]) : JList[T] = 
		LazyList.decorate(list, FactoryUtils.instantiateFactory(m.erasure))
			.asInstanceOf[JList[T]]
	
}