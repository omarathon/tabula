package uk.ac.warwick.tabula.helpers

import org.slf4j.LoggerFactory
import uk.ac.warwick.tabula.EarlyRequestInfo
import uk.ac.warwick.tabula.helpers.RequestLevelCache.Cache

import scala.collection.mutable

trait RequestLevelCaching[A, B] {
	def cache: Option[Cache[A, B]] = RequestLevelCache.cache(getClass.getName)
	def cachedBy(key: A)(default: => B): B = RequestLevelCache.cachedBy(getClass.getName, key)(default)
}

class RequestLevelCachingError extends Error

object RequestLevelCache {
	type Cache[A, B] = mutable.Map[A, B]

	private[helpers] val requestLevelCachingLogger = LoggerFactory.getLogger(classOf[RequestLevelCache])

	// Uses EarlyRequestInfo which is available before the full RequestInfo, since we need some caching
	// for permissions lookups to create the CurrentUser.
	def cache[A, B](cacheName: String): Option[Cache[A, B]] =
		EarlyRequestInfo.fromThread.map { _.requestLevelCache.getCacheByName[A, B](cacheName) }

	def cachedBy[A, B](cacheName: String, key: A)(default: => B): B =
		EarlyRequestInfo.fromThread.map { _.requestLevelCache.getCacheByName[A, B](cacheName) } match {
			case Some(cache) => cache.getOrElseUpdate(key, default)
			case _ =>
				// Include error to get stack trace
				requestLevelCachingLogger.debug("Calling a request level cache outside of a request", new RequestLevelCachingError)
				default
		}
}

class RequestLevelCache {
	import RequestLevelCache._

	private val cacheMap = mutable.Map[String, Cache[_, _]]()

	def getCacheByName[A, B](name: String): Cache[A, B] = cacheMap.get(name) match {
		case Some(cache: Cache[_, _]) => cache.asInstanceOf[Cache[A, B]]
		case _ =>
			val cache = mutable.Map[A, B]()

			// If we've put it in the map in some other thread, we return that - otherwise return the one we've just put in
			cacheMap.put(name, cache).getOrElse(cache).asInstanceOf[Cache[A, B]]
	}

	def shutdown() {
		for ((_, cache) <- cacheMap) {
			cache.clear()
		}
		cacheMap.clear()
	}
}