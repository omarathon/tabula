package uk.ac.warwick.courses

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
	type JList[V] = java.util.List[V]
	type JMap[K,V] = java.util.Map[K,V]
	type JSet[V] = java.util.Set[V]
	type JInteger = java.lang.Integer
	type JLong = java.lang.Long
	
	/**
	 * Converts an Option[Boolean] to a Java Boolean, by interpreting
	 * None as null. 
	 */
	protected implicit def ToJBoolean(b:Option[Boolean]) = b map (b => b: JBoolean) orNull
}

object JavaImports extends JavaImports