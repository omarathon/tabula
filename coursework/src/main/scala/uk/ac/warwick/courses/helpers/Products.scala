package uk.ac.warwick.courses.helpers

/**
 * Methods that work on Products, which include Tuples such
 * as Tuple2 (which is what you get when you do (a -> b) and is
 * the type of each entry in a Map).
 */
object Products {
	/** Is the first value of this Product2 null? */
	def nullKey[A,B](p:Product2[A,B]) = p._1 == null
	
	/** Is the second value of this Product2 null?
	 *  Useful for filtering a Map:
	 * 
	 *	  map.filterNot(nullValue)
	 */
	def nullValue[A,B](p:Product2[A,B]) = p._2 == null

	def toKey[A,B](p: Product2[A,B]) = p._1
	def toValue[A,B](p: Product2[A,B]) = p._2
}
