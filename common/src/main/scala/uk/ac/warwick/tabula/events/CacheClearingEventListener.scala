package uk.ac.warwick.tabula.events

import uk.ac.warwick.tabula.commands.cm2.MarkerWorkflowCache
import uk.ac.warwick.tabula.commands.cm2.assignments.AssignmentProgressCache
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.util.cache.{CacheStore, Caches}

class CacheClearingEventListener extends EventListener with AutowiringCacheStrategyComponent {

	lazy val markerWorkflowCacheStore: CacheStore[MarkerWorkflowCache.AssignmentId, MarkerWorkflowCache.Json] =
		Caches.newCacheStore(MarkerWorkflowCache.CacheName, MarkerWorkflowCache.CacheExpiryTime, cacheStrategy)

	lazy val assignmentProgressCacheStore: CacheStore[AssignmentProgressCache.AssignmentId, AssignmentProgressCache.Json] =
		Caches.newCacheStore(AssignmentProgressCache.CacheName, AssignmentProgressCache.CacheExpiryTime, cacheStrategy)

	override def beforeCommand(event: Event): Unit = {}
	override def onException(event: Event, exception: Throwable): Unit = {}

	override def afterCommand(event: Event, returnValue: Any, beforeEvent: Event): Unit = {
		val data = beforeEvent.extra ++ event.extra
		data.get("assignment") match {
			case Some(id: String) if !event.readOnly =>
				markerWorkflowCacheStore.remove(id)
				assignmentProgressCacheStore.remove(id)

			case _ => // Do nothing
		}
	}

}
