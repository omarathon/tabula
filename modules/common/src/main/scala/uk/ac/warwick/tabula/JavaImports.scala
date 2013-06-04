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
	type JArrayList[A] = java.util.ArrayList[A]
	type JMap[K, V] = java.util.Map[K, V]
	type JSet[A] = java.util.Set[A]
	type JInteger = java.lang.Integer
	type JLong = java.lang.Long
	
	def JBoolean(b: Option[Boolean]) = ToJBoolean(b)
	def JList[A](items: A*) = mutable.Seq(items: _*).asJava
	def JMap[K, V](elems: (K, V)*) = mutable.Map[K, V](elems: _*).asJava
	def JSet[A](items: A*) = mutable.Set(items: _*).asJava
	def JInteger(i: Option[Int]) = ToJInteger(i)
	def JLong(l: Option[Long]) = ToJLong(l)

	/**
	 * Converts an Option[Boolean] to a Java Boolean, by interpreting
	 * None as null.
	 */
	protected implicit def ToJBoolean(b: Option[Boolean]) = (b map (b => b: JBoolean)).orNull

	/**
	 * Converts an Option[Int] to a Java Integer, by interpreting
	 * None as null.
	 */
	protected implicit def ToJInteger(i: Option[Int]) = (i map (i => i: JInteger)).orNull

	/**
	 * Converts an Option[Long] to a Java Long, by interpreting
	 * None as null.
	 */
	protected implicit def ToJLong(l: Option[Long]) = (l map (l => l: JLong)).orNull
	
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

		def apply[A](orig: List[A]): java.util.ArrayList[A] = {
			val list = new java.util.ArrayList[A]()
			if (!orig.isEmpty) list.addAll(orig.asJavaCollection)
			list
		}

		def apply[A](orig: JList[A]): java.util.ArrayList[A] = {
			val list = new java.util.ArrayList[A]()
			if (!orig.isEmpty) list.addAll(orig)
			list
		}
	}
	
	/**
	 * Allows you to create an empty Java HashSet, useful as an initial
	 * value for a variable that needs a mutable JSet.
	 */
	object JHashSet {
		def apply[A](elements: A*): java.util.HashSet[A] = {
			val set = new java.util.HashSet[A]()
			if (!elements.isEmpty) set.addAll(elements.asJavaCollection)
			set
		}

		def apply[A](orig: Set[A]): java.util.HashSet[A] = {
			val set = new java.util.HashSet[A]()
			if (!orig.isEmpty) set.addAll(orig.asJavaCollection)
			set
		}

		def apply[A](orig: JSet[A]): java.util.HashSet[A] = {
			val set = new java.util.HashSet[A]()
			if (!orig.isEmpty) set.addAll(orig)
			set
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