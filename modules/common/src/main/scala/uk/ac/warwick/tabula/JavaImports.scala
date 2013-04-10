package uk.ac.warwick.tabula
import collection.JavaConverters._
import collection.mutable
import language.implicitConversions

/**
 * Quick way to expose a bunch of Java type names under
 * different names like JBoolean, so they can be differentiated
 * from the Scala types.
 *
 * You can either import:
 * {{{
 * import uk.ac.warwick.coures.JavaImports._
 * }}}
 * or you can add the JavaImports trait to your class:
 * {{{
 * class HatStand extends Furniture with JavaImports
 * }}}
 */

trait JavaImports {
	type JBoolean = java.lang.Boolean
	type JList[A] = java.util.List[A]
	type JMap[K, V] = java.util.Map[K, V]
	type JSet[A] = java.util.Set[A]
	type JInteger = java.lang.Integer
	type JLong = java.lang.Long
	
	def JSet[A](items: A*) = mutable.Set(items: _*).asJava
	def JMap[K, V]() = mutable.Map[K, V]().asJava

	/**
	 * Converts an Option[Boolean] to a Java Boolean, by interpreting
	 * None as null.
	 */
	protected implicit def ToJBoolean(b: Option[Boolean]) = (b map (b => b: JBoolean)).orNull

	/**
	 * Converts an Option[Integer] to a Java Integer, by interpreting
	 * None as null.
	 */
	protected implicit def ToJInteger(b: Option[Int]) = (b map (b => b: JInteger)).orNull
	
	/**
	 * Allows you to create an empty Java ArrayList, useful as an initial
	 * value for a variable that needs a mutable JList.
	 */
	object JArrayList {
		def apply[A](elements: A*): java.util.ArrayList[A] = {
			val list = new java.util.ArrayList[A]()
			if (!elements.isEmpty) list.addAll(elements.asJavaCollection)
			list
		}
	}
	
	/**
	 * Allows you to create an empty Java HashMap, useful as an initial
	 * value for a variable that needs a mutable JMap.
	 */
	object JHashMap {
		def apply[A, B](elements: (A, B)*): java.util.HashMap[A, B] = {
			val map = new java.util.HashMap[A, B]()
			elements foreach { case (key, value) => map.put(key, value) }
			map
		}
	}
}

object JavaImports extends JavaImports