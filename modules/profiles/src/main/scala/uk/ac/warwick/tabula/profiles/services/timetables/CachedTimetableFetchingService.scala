package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.util.cache.{CacheEntryFactory, Caches}
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import java.util
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports.JMap


/**
 * A wrapper around a TimetableFetchingService that stores the resulting TimetableEvents in an ehCache disk-backed
 * cache, thus allowing us not to thrash Syllabus+ too badly
 *
 */
class CachedTimetableFetchingService(delegate:TimetableFetchingService) extends TimetableFetchingService{

	val CacheExpiryTime = 1000*60*60*6 // 6 hours in ms

	sealed trait TimetableCacheKey extends Serializable{
		val id:String
	}
	case class StudentKey(val id:String) extends TimetableCacheKey
	case class StaffKey(val id:String) extends TimetableCacheKey
	case class CourseKey(val id:String) extends TimetableCacheKey
	case class RoomKey(val id:String) extends TimetableCacheKey
	case class ModuleKey(val id:String) extends TimetableCacheKey

	/**
	 * Yukkiness Ahead.
	 *
	 * Caches requre their payload to be serializable, but Scala's Seq trait isn't marked as serializable. Most of
	 * the Seq implementations (Vector and LinkedList in particular,which are the defaults returned by Seq() and List() )
	 * _are_ serializable, so it should be safe to match the Seqs to "Seq with Serializable" and assume it will never fail.
	 *
	 */
	type EventList = Seq[TimetableEvent] with java.io.Serializable

	val cacheEntryFactory = new CacheEntryFactory[TimetableCacheKey,EventList] {

	 private def toEventList(events: Seq[TimetableEvent]):EventList = {
			events match {
				// can't use "case v: EventList" because the type inference engine in 2.10 can't cope.
				case v: Seq[TimetableEvent] with java.io.Serializable => v
				case _ => throw new RuntimeException("Unserializable collection returned from TimetableFetchingService")
			}
		}

		def create(key:TimetableCacheKey):EventList = toEventList(key match {
			case StudentKey(id)=>delegate.getTimetableForStudent(id)
			case StaffKey(id)=>delegate.getTimetableForStaff(id)
			case CourseKey(id)=>delegate.getTimetableForCourse(id)
			case RoomKey(id)=>delegate.getTimetableForRoom(id)
			case ModuleKey(id)=>delegate.getTimetableForModule(id)
		})

		def create(keys: util.List[TimetableCacheKey]): util.Map[TimetableCacheKey, EventList] = {
			JMap(keys.asScala.map(id => (id, create(id))): _*)
		}
		def isSupportsMultiLookups: Boolean = true
		def shouldBeCached(response: EventList): Boolean = true
	}
	// rather than using 5 little caches, use one big one with a composite key
	val timetableCache = Caches.newCache("SyllabusPlusTimetables", cacheEntryFactory, CacheExpiryTime, CacheStrategy.EhCacheIfAvailable)

	def getTimetableForStudent(universityId: String): Seq[TimetableEvent] = timetableCache.get(StudentKey(universityId))
	def getTimetableForModule(moduleCode: String): Seq[TimetableEvent] = timetableCache.get(ModuleKey(moduleCode))
	def getTimetableForCourse(courseCode: String): Seq[TimetableEvent] = timetableCache.get(CourseKey(courseCode))
	def getTimetableForRoom(roomName: String): Seq[TimetableEvent] = timetableCache.get(RoomKey(roomName))
	def getTimetableForStaff(universityId: String): Seq[TimetableEvent] = timetableCache.get(StaffKey(universityId))
}

