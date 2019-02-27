package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.services.permissions.CacheStrategyComponent
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.util.cache.{Caches, SingularCacheEntryFactoryWithDataInitialisation}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait CachedEventOccurrenceSourceComponent { self: CacheStrategyComponent =>

	case class CacheData(
		member: Member,
		currentUser: CurrentUser,
		context: TimetableEvent.Context,
		start: LocalDate,
		end: LocalDate) {
		def cacheKey = s"${member.userId}:$start-$end"
	}

	class CachedEventOccurrenceSource(cacheName: String, delegate: EventOccurrenceSource) extends EventOccurrenceSource {

		private val ttl = 2.hours.toSeconds

		private val factory = new SingularCacheEntryFactoryWithDataInitialisation[String, EventOccurrenceList, CacheData] {
			override def create(key: String, data: CacheData): EventOccurrenceList = {
				import data._
				Await.result(
					delegate.occurrencesFor(member, currentUser, context, start, end),
					Duration.Inf // we can rely on the default timeouts in the HTTP Client
				)
			}
			override def shouldBeCached(list: EventOccurrenceList): Boolean = true
		}

		private val cache = Caches.newDataInitialisatingCache(cacheName, factory, ttl, cacheStrategy)

		override def occurrencesFor(
			member: Member,
			currentUser: CurrentUser,
			context: TimetableEvent.Context,
			start: LocalDate,
			end: LocalDate): Future[EventOccurrenceList] = Future {
			val data = CacheData(member, currentUser, context, start, end)
			cache.get(data.cacheKey, data)
		}
	}

}
