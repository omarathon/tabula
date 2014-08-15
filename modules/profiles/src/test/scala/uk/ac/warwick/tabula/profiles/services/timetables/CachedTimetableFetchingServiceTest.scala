package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.LocalTime
import uk.ac.warwick.util.cache.{HashMapCacheStore, Caches}
import uk.ac.warwick.tabula.timetables.{TimetableEventType, TimetableEvent}
import net.spy.memcached.transcoders.SerializingTranscoder
import org.junit.Before

class CachedTimetableFetchingServiceTest  extends TestBase with Mockito{

	private trait Fixture{

		val studentId = "studentId"
		val studentEvents = Seq(new TimetableEvent("test","test","test",TimetableEventType.Lecture,Nil,DayOfWeek.Monday,new LocalTime,new LocalTime,None,None,Nil, AcademicYear(2013)))
		val delegate = mock[TimetableFetchingService]

		delegate.getTimetableForStudent(studentId) returns studentEvents
		
		val cache = new CachedTimetableFetchingService(delegate, "cacheName")
	}

	@Before def clearCaches {
		HashMapCacheStore.clearAll()
	}

	@Test
	def firstRequestIsPassedThrough(){new Fixture {
		cache.getTimetableForStudent(studentId) should be(studentEvents)
		there was one (delegate).getTimetableForStudent(studentId)
	}}

	@Test
	def repeatedRequestsAreCached(){new Fixture {
		cache.getTimetableForStudent(studentId) should be(studentEvents)
		cache.getTimetableForStudent(studentId) should be(studentEvents)
		there was one(delegate).getTimetableForStudent(studentId)
	}}

	@Test
	def keyTypesAreDiscriminated() { new Fixture {
		// deliberately use the student ID to look up some staff events. The cache key should be the ID + the type of
		// request (staff, student, room, etc) so we should get different results back for student and staff

		val staffEvents = Seq(new TimetableEvent("test2", "test2","test2",TimetableEventType.Lecture,Nil,DayOfWeek.Monday,new LocalTime,new LocalTime,None,None,Nil, AcademicYear(2013)))
		delegate.getTimetableForStaff(studentId) returns staffEvents

		cache.getTimetableForStudent(studentId)  should be(studentEvents)
		cache.getTimetableForStudent(studentId)  should be(studentEvents)
		cache.getTimetableForStaff(studentId)  should be(staffEvents)
		cache.getTimetableForStaff(studentId)  should be(staffEvents)
		there was one (delegate).getTimetableForStudent(studentId)
		there was one (delegate).getTimetableForStaff(studentId)

	}}

	@Test
	def serialization() {
		val transcoder: SerializingTranscoder = new SerializingTranscoder
		transcoder.encode(TimetableCacheKey.StudentKey("0672089"))
		transcoder.encode(TimetableCacheKey.StudentKey(""))
		transcoder.encode(TimetableCacheKey.ModuleKey("cs118"))
	}

}
