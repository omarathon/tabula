package uk.ac.warwick.tabula.helpers

import scala.collection.mutable

/**
 * Methods for generating a lazy map that will calculate the value behind
 * each key once and then never again.
 */
object LazyMaps {
	
	/**
	 * Return a HashMap which will populate empty key values using the given
	 * factory.
	 */
    def create[K,V](factory: (K)=>(V)) = new mutable.HashMap[K,V] {
    	override def get(key: K) = 
    		Option( superGetOrUpdate(key, { factory(key) }) )
    	
    	// same as getOrElseUpdate, except we call super.get to prevent a stack overflow.
    	private def superGetOrUpdate(key:K, op: =>V) = 
    		super.get(key) match {
              case Some(v) => v
              case None => val d = op; this(key) = d; d
            }
    }
}