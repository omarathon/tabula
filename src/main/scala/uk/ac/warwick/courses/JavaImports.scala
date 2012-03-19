package uk.ac.warwick.courses

/**
 * Quick way to expose a bunch of Java type names under
 * different names like JBoolean, so they can be differentiated
 * from the Scala types.
 * 
 * You can either import uk.ac.warwick.coures.JavaImports._,
 * or you can add the JavaImports trait to your class.
 */

trait JavaImports {
	type JBoolean = java.lang.Boolean
	type JList[V] = java.util.List[V]
	type JMap[K,V] = java.util.Map[K,V]
	type JSet[V] = java.util.Set[V]
	type JInteger = java.lang.Integer
	type JLong = java.lang.Long
}

object JavaImports extends JavaImports