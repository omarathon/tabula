package uk.ac.warwick.tabula.services.timetables

import java.util.concurrent.TimeUnit

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.tabula.services.timetables.TimetableCacheKey._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.util.cache._
import uk.ac.warwick.util.collections.Pair

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
 * A wrapper around a TimetableFetchingService that stores the resulting TimetableEvents in a
 * cache, thus allowing us not to thrash Syllabus+ too badly
 *
 */
class CachedPartialTimetableFetchingService(delegate: PartialTimetableFetchingService, cacheName: String) extends PartialTimetableFetchingService with AutowiringCacheStrategyComponent {

	val CacheExpiryTime = 60 * 60 * 6 // 6 hours in seconds
	val FetchTimeout = 15.seconds

	val cacheEntryFactory = new CacheEntryFactory[TimetableCacheKey, EventList] {

		def create(key:TimetableCacheKey): EventList = {
			val result = key match {
				case StudentKey(id) => delegate match {
					case delegate: StudentTimetableFetchingService => delegate.getTimetableForStudent(id)
					case _ => throw new UnsupportedOperationException("Delegate does not support fetching student timetables")
				}
				case StaffKey(id)   => delegate match {
					case delegate: StaffTimetableFetchingService => delegate.getTimetableForStaff(id)
					case _ => throw new UnsupportedOperationException("Delegate does not support fetching staff timetables")
				}
				case CourseKey(id)  => delegate match {
					case delegate: CourseTimetableFetchingService => delegate.getTimetableForCourse(id)
					case _ => throw new UnsupportedOperationException("Delegate does not support fetching course timetables")
				}
				case RoomKey(id)    => delegate match {
					case delegate: RoomTimetableFetchingService => delegate.getTimetableForRoom(id)
					case _ => throw new UnsupportedOperationException("Delegate does not support fetching room timetables")
				}
				case ModuleKey(id)  => delegate match {
					case delegate: ModuleTimetableFetchingService => delegate.getTimetableForModule(id)
					case _ => throw new UnsupportedOperationException("Delegate does not support fetching module timetables")
				}
			}

			Try(Await.result(result, FetchTimeout)) match {
				case Success(ev) => ev
				case Failure(e) => throw new CacheEntryUpdateException(e)
			}
		}

		def create(keys: JList[TimetableCacheKey]): JMap[TimetableCacheKey, EventList] = {
			JMap(keys.asScala.map(id => (id, create(id))): _*)
		}
		def isSupportsMultiLookups: Boolean = true
		def shouldBeCached(response: EventList): Boolean = true
	}

	// rather than using 5 little caches, use one big one with a composite key
	lazy val timetableCache = {
		val cache = Caches.newCache(cacheName, cacheEntryFactory, CacheExpiryTime * 4, cacheStrategy)
		// serve stale data if we have it while we update in the background
		cache.setExpiryStrategy(new TTLCacheExpiryStrategy[TimetableCacheKey, EventList] {
			override def getTTL(entry: CacheEntry[TimetableCacheKey, EventList]): Pair[Number, TimeUnit] = Pair.of(CacheExpiryTime * 4, TimeUnit.SECONDS)
			override def isStale(entry: CacheEntry[TimetableCacheKey, EventList]): Boolean = (entry.getTimestamp + CacheExpiryTime * 1000) <= DateTime.now.getMillis
		})
		cache.setAsynchronousUpdateEnabled(true)
		cache
	}

	// Unwraps the CacheEntryUpdateException into its cause, for case matching
	private def toFuture(eventList: Try[EventList]) = Future.fromTry(
		eventList.recoverWith { case e: CacheEntryUpdateException => Failure(e.getCause) }
	)

	def getTimetableForStudent(universityId: String) = toFuture(Try(timetableCache.get(StudentKey(universityId))))
	def getTimetableForModule(moduleCode: String) = toFuture(Try(timetableCache.get(ModuleKey(moduleCode))))
	def getTimetableForCourse(courseCode: String) = toFuture(Try(timetableCache.get(CourseKey(courseCode))))
	def getTimetableForRoom(roomName: String) = toFuture(Try(timetableCache.get(RoomKey(roomName))))
	def getTimetableForStaff(universityId: String) = toFuture(Try(timetableCache.get(StaffKey(universityId))))

}

class CachedStudentTimetableFetchingService(delegate: StudentTimetableFetchingService, cacheName: String)
	extends CachedPartialTimetableFetchingService(delegate, cacheName) with StudentTimetableFetchingService

class CachedModuleTimetableFetchingService(delegate: ModuleTimetableFetchingService, cacheName: String)
	extends CachedPartialTimetableFetchingService(delegate, cacheName) with ModuleTimetableFetchingService

class CachedCourseTimetableFetchingService(delegate: CourseTimetableFetchingService, cacheName: String)
	extends CachedPartialTimetableFetchingService(delegate, cacheName) with CourseTimetableFetchingService

class CachedRoomTimetableFetchingService(delegate: RoomTimetableFetchingService, cacheName: String)
	extends CachedPartialTimetableFetchingService(delegate, cacheName) with RoomTimetableFetchingService

class CachedStaffTimetableFetchingService(delegate: StaffTimetableFetchingService, cacheName: String)
	extends CachedPartialTimetableFetchingService(delegate, cacheName) with StaffTimetableFetchingService

class CachedStaffAndStudentTimetableFetchingService(delegate: StudentTimetableFetchingService with StaffTimetableFetchingService, cacheName: String)
	extends CachedPartialTimetableFetchingService(delegate, cacheName) with StudentTimetableFetchingService with StaffTimetableFetchingService

class CachedCompleteTimetableFetchingService(delegate: CompleteTimetableFetchingService, cacheName: String)
	extends CachedPartialTimetableFetchingService(delegate, cacheName) with CompleteTimetableFetchingService

@SerialVersionUID(3326840601345l) sealed trait TimetableCacheKey extends Serializable {
	val id: String
}

object TimetableCacheKey {
	case class StudentKey(id: String) extends TimetableCacheKey
	case class StaffKey(id: String) extends TimetableCacheKey
	case class CourseKey(id: String) extends TimetableCacheKey
	case class RoomKey(id: String) extends TimetableCacheKey
	case class ModuleKey(id: String) extends TimetableCacheKey
}