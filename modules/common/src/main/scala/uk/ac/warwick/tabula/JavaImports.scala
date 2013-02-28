package uk.ac.warwick.tabula
import collection.JavaConverters._
import collection.mutable

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
	protected implicit def ToJBoolean(b: Option[Boolean]) = b map (b => b: JBoolean) orNull

	/**
	 * Converts an Option[Integer] to a Java Integer, by interpreting
	 * None as null.
	 */
	protected implicit def ToJInteger(b: Option[Int]) = b map (b => b: JInteger) orNull
}

object JavaImports extends JavaImports