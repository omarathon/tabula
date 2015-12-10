package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.util.cache.{CacheEntryUpdateException, CacheEntryFactory, Caches}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.tabula.services.timetables.TimetableCacheKey._

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

/**
 * A wrapper around a TimetableFetchingService that stores the resulting TimetableEvents in a
 * cache, thus allowing us not to thrash Syllabus+ too badly
 *
 */
class CachedPartialTimetableFetchingService(delegate: PartialTimetableFetchingService, cacheName: String) extends PartialTimetableFetchingService with AutowiringCacheStrategyComponent {

	val CacheExpiryTime = 60 * 60 * 6 // 6 hours in seconds
	val FetchTimeout = 15.seconds

	/**
	 * Yukkiness Ahead.
	 *
	 * Caches requre their payload to be serializable, but Scala's Seq trait isn't marked as serializable. Most of
	 * the Seq implementations (Vector and LinkedList in particular, which are the defaults returned by Seq() and List() )
	 * _are_ serializable, so it should be safe to match the Seqs to "Seq with Serializable" and assume it will never fail.
	 */
	type EventList = Seq[TimetableEvent] with java.io.Serializable

	val cacheEntryFactory = new CacheEntryFactory[TimetableCacheKey, EventList] {

		private def toEventList(events: Seq[TimetableEvent]): EventList = {
			events match {
				// can't use "case v: EventList" because the type inference engine in 2.10 can't cope.
				case v: Seq[TimetableEvent] with java.io.Serializable => v
				case _ => throw new RuntimeException("Unserializable collection returned from TimetableFetchingService")
			}
		}

		def create(key:TimetableCacheKey): EventList = {
			val result = (key match {
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
			}).map(toEventList)

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
		val cache = Caches.newCache(cacheName, cacheEntryFactory, CacheExpiryTime, cacheStrategy)
		// serve stale data if we have it while we update in the background
		cache.setAsynchronousUpdateEnabled(true)
		cache
	}

	def getTimetableForStudent(universityId: String) = Future.fromTry(Try(timetableCache.get(StudentKey(universityId))))
	def getTimetableForModule(moduleCode: String) = Future.fromTry(Try(timetableCache.get(ModuleKey(moduleCode))))
	def getTimetableForCourse(courseCode: String) = Future.fromTry(Try(timetableCache.get(CourseKey(courseCode))))
	def getTimetableForRoom(roomName: String) = Future.fromTry(Try(timetableCache.get(RoomKey(roomName))))
	def getTimetableForStaff(universityId: String) = Future.fromTry(Try(timetableCache.get(StaffKey(universityId))))

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