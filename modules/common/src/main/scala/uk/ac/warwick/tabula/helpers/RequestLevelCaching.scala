package uk.ac.warwick.tabula.helpers

import scala.collection.mutable
import uk.ac.warwick.tabula.{EarlyRequestInfo, RequestInfo}

trait RequestLevelCaching[A, B] extends Logging {

	// Uses EarlyRequestInfo which is available before the full RequestInfo, since we need some caching
	// for permissions lookups to create the CurrentUser.
	def cache = EarlyRequestInfo.fromThread map { _.requestLevelCache.getCacheByName[A, B](getClass.getName) }
	
	def cachedBy(key: A)(default: => B) = cache match {
		case Some(cache) => cache.getOrElseUpdate(key, default)
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
		case Some(cache: Cache[_, _]) => cache.asInstanceOf[Cache[A, B]]
		case _ => {
			val cache = mutable.Map[A, B]()
			
			// If we've put it in the map in some other thread, we return that - otherwise return the one we've just put in
			(cacheMap.put(name, cache) getOrElse (cache)).asInstanceOf[Cache[A, B]]
		}
	}
	
	def shutdown() {
		for ((name, cache) <- cacheMap) {
			cache.clear()
		}
		cacheMap.clear()
	}
	
}