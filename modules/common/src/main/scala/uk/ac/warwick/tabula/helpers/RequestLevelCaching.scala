package uk.ac.warwick.tabula.helpers

import scala.collection.mutable
import uk.ac.warwick.tabula.RequestInfo

trait RequestLevelCaching[A, B] extends Logging {
	import RequestLevelCache._
	
	def cache = RequestInfo.fromThread map { _.requestLevelCache.getCacheByName[A, B](getClass.getName) }
	
	def cachedBy[B1 >: B](key: A, default: => B1) = cache match {
		case Some(cache) => cache.getOrElse(key, default)
		case _ => {
			logger.warn("Tried to call a request level cache outside of a request!")
			default
		}
	}

}

object RequestLevelCache {
	type Cache[A, B] = mutable.Map[A, B]
}

class RequestLevelCache {
	import RequestLevelCache._
	
	private val cacheMap = mutable.Map[String, Cache[_, _]]()
	
	def getCacheByName[A, B](name: String): Cache[A, B] = cacheMap.get(name) match {
		case cache: Cache[_, _] => cache.asInstanceOf[Cache[A, B]]
		case _ => {
			val cache = mutable.Map[A, B]()
			
			// If we've put it in the map in some other thread, we return that - otherwise return the one we've just put in
			(cacheMap.put(name, cache) getOrElse (cache)).asInstanceOf[Cache[A, B]]
		}
	}
	
	def shutdown = {
		cacheMap.seq.foreach(_._2.clear)
		cacheMap.clear
	}
	
}