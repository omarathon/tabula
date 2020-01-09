package uk.ac.warwick.tabula.helpers

import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import org.slf4j.LoggerFactory
import uk.ac.warwick.tabula.EarlyRequestInfo
import uk.ac.warwick.tabula.helpers.RequestLevelCache.Cache

import scala.jdk.CollectionConverters._
import scala.collection.mutable

trait RequestLevelCaching[A, B] {
  def cache: Option[Cache[A, B]] = RequestLevelCache.cache(getClass.getName)

  def cachedBy(key: A)(default: => B): B = RequestLevelCache.cachedBy(getClass.getName, key)(default)

  // This uses an IdentityHashMap instead of the regular HashMap, which uses reference equality rather than equals()/hashCode()
  def cachedByIdentity(key: A)(default: => B): B = RequestLevelCache.cachedBy(getClass.getName, key)(default)

  def evictAll(): Unit = RequestLevelCache.evictAll()
}

class RequestLevelCachingError extends Error

object RequestLevelCache {
  type Cache[A, B] = mutable.Map[A, B]

  private[helpers] val requestLevelCachingLogger = LoggerFactory.getLogger(classOf[RequestLevelCache])

  // Uses EarlyRequestInfo which is available before the full RequestInfo, since we need some caching
  // for permissions lookups to create the CurrentUser.
  def cache[A, B](cacheName: String): Option[Cache[A, B]] =
    EarlyRequestInfo.fromThread.map {
      _.requestLevelCache.getCacheByName[A, B](cacheName)
    }

  def cachedBy[A, B](cacheName: String, key: A)(default: => B): B =
    EarlyRequestInfo.fromThread.map {
      _.requestLevelCache.getCacheByName[A, B](cacheName)
    } match {
      case Some(cache) => {
        lazy val op = default
        try {
          cache.getOrElseUpdate(key, op)
        } catch {
          case e: NullPointerException => op
        }
      }
      case _ =>
        // Include error to get stack trace
        requestLevelCachingLogger.debug("Calling a request level cache outside of a request", new RequestLevelCachingError)
        default
    }

  def cachedByIdentity[A, B](cacheName: String, key: A)(default: => B): B =
    EarlyRequestInfo.fromThread.map {
      _.requestLevelCache.getIdentityCacheByName[A, B](cacheName)
    } match {
      case Some(cache) => cache.getOrElseUpdate(key, default)
      case _ =>
        // Include error to get stack trace
        requestLevelCachingLogger.debug("Calling a request level cache outside of a request", new RequestLevelCachingError)
        default
    }

  def evictAll(): Unit = EarlyRequestInfo.fromThread.foreach(_.requestLevelCache.shutdown())
}

class RequestLevelCache {

  import RequestLevelCache._

  //TAB-7331 (Related with CPU Spike)
  private val cacheMap: scala.collection.concurrent.Map[String, Cache[_, _]] = new ConcurrentHashMap[String, Cache[_, _]]().asScala

  def getCacheByName[A, B](name: String): Cache[A, B] = cacheMap.get(name) match {
    case Some(cache: Cache[_, _]) => cache.asInstanceOf[Cache[A, B]]
    case _ =>
      val cache: scala.collection.concurrent.Map[A, B] = new ConcurrentHashMap[A, B]().asScala
      // If we've put it in the map in some other thread, we return that - otherwise return the one we've just put in
      cacheMap.put(name, cache).getOrElse(cache).asInstanceOf[Cache[A, B]]
  }

  def getIdentityCacheByName[A, B](name: String): Cache[A, B] = cacheMap.get(name) match {
    case Some(cache: Cache[_, _]) => cache.asInstanceOf[Cache[A, B]]
    case _ =>
      val cache = Collections.synchronizedMap(new util.IdentityHashMap[A, B]()).asScala
      // If we've put it in the map in some other thread, we return that - otherwise return the one we've just put in
      cacheMap.put(name, cache).getOrElse(cache).asInstanceOf[Cache[A, B]]
  }

  def shutdown(): Unit = {
    for ((_, cache) <- cacheMap) {
      cache.clear()
    }
    cacheMap.clear()
  }
}
