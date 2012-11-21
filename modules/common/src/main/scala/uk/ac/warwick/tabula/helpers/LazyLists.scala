package uk.ac.warwick.tabula.helpers

import org.apache.commons.collections.list.LazyList
import org.apache.commons.collections.FactoryUtils
import uk.ac.warwick.tabula.JavaImports._

/**
 * Lazy lists are useful in Spring forms when you have a list of rich class objects
 * because Spring won't create empty objects for you.
 *
 * e.g. myCommand.widgets[5].name=rob
 *
 * Spring will call getWidgets().get(5).setName("rob"). A LazyFactory will create an
 * empty widget at index 5 so that there isn't an NPE.
 */
object LazyLists {

	/**
	 * Creates a lazy list (optionally based on an existing list) that will create
	 * elements on demand using the default no-arg constructor for the class.
	 */
	def simpleFactory[T](list: JList[T] = ArrayList[T]())(implicit m: Manifest[T]): JList[T] =
		LazyList.decorate(list, FactoryUtils.instantiateFactory(m.erasure))
			.asInstanceOf[JList[T]]

}