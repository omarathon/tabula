package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.helpers.{Futures, Logging}
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.Futures._

import scala.concurrent.Future

trait PartialTimetableFetchingService

trait StudentTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForStudent(universityId: String): Future[Seq[TimetableEvent]]
}

trait ModuleTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForModule(moduleCode: String): Future[Seq[TimetableEvent]]
}

trait CourseTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForCourse(courseCode: String): Future[Seq[TimetableEvent]]
}

trait RoomTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForRoom(roomName: String): Future[Seq[TimetableEvent]]
}

trait StaffTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForStaff(universityId: String): Future[Seq[TimetableEvent]]
}

trait CompleteTimetableFetchingService
	extends StudentTimetableFetchingService
		with ModuleTimetableFetchingService
		with CourseTimetableFetchingService
		with RoomTimetableFetchingService
		with StaffTimetableFetchingService

trait StudentTimetableFetchingServiceComponent {
	def timetableFetchingService: StudentTimetableFetchingService
}

trait ModuleTimetableFetchingServiceComponent {
	def timetableFetchingService: ModuleTimetableFetchingService
}

trait CourseTimetableFetchingServiceComponent {
	def timetableFetchingService: CourseTimetableFetchingService
}

trait RoomTimetableFetchingServiceComponent {
	def timetableFetchingService: RoomTimetableFetchingService
}

trait StaffTimetableFetchingServiceComponent {
	def timetableFetchingService: StaffTimetableFetchingService
}

trait StaffAndStudentTimetableFetchingServiceComponent extends StudentTimetableFetchingServiceComponent with StaffTimetableFetchingServiceComponent {
	def timetableFetchingService: StudentTimetableFetchingService with StaffTimetableFetchingService
}

trait CompleteTimetableFetchingServiceComponent
	extends StaffAndStudentTimetableFetchingServiceComponent
		with ModuleTimetableFetchingServiceComponent
		with CourseTimetableFetchingServiceComponent
		with RoomTimetableFetchingServiceComponent {
	def timetableFetchingService: CompleteTimetableFetchingService
}

trait CombinedHttpTimetableFetchingServiceComponent extends CompleteTimetableFetchingServiceComponent {
	self: ScientiaConfigurationComponent with CelcatConfigurationComponent =>

	lazy val timetableFetchingService = new CombinedTimetableFetchingService(
		ScientiaHttpTimetableFetchingService(scientiaConfiguration),
		CelcatHttpTimetableFetchingService(celcatConfiguration)
	)

}

class CombinedTimetableFetchingService(services: PartialTimetableFetchingService*) extends CompleteTimetableFetchingService with Logging {

	def mergeDuplicates(events: Seq[TimetableEvent]): Seq[TimetableEvent] = {
		// If an event runs on the same day, between the same times, in the same weeks, of the same type, on the same module, it is the same
		events.groupBy { event => (event.year, event.day, event.startTime, event.endTime, event.weekRanges, event.eventType, event.parent.shortName) }
			.mapValues {
				case event :: Nil => event
				case groupedEvents =>
					val event = groupedEvents.head
					TimetableEvent(
						event.uid,
						groupedEvents.flatMap { _.name.maybeText }.headOption.getOrElse(""),
						groupedEvents.flatMap { _.title.maybeText }.headOption.getOrElse(""),
						groupedEvents.flatMap { _.description.maybeText }.headOption.getOrElse(""),
						event.eventType,
						event.weekRanges,
						event.day,
						event.startTime,
						event.endTime,
						groupedEvents.flatMap { _.location }.headOption,
						event.parent,
						groupedEvents.flatMap { _.comments }.headOption,
						groupedEvents.flatMap { _.staff }.distinct,
						groupedEvents.flatMap { _.students }.distinct,
						event.year
					)
			}
			.values.toSeq
	}

	def getTimetableForStudent(universityId: String) =
		Futures.flatten(services
			.collect { case service: StudentTimetableFetchingService => service }
			.map {
				// On downstream failures, just return Nil
				_.getTimetableForStudent(universityId).recover { case t => logger.warn("Error fetching timetable", t); Nil }
			}
		).map(mergeDuplicates)

	def getTimetableForModule(moduleCode: String) =
		Futures.flatten(services
			.collect { case service: ModuleTimetableFetchingService => service }
			.map {
				// On downstream failures, just return Nil
				_.getTimetableForModule(moduleCode).recover { case t => logger.warn("Error fetching timetable", t); Nil }
			}
		).map(mergeDuplicates)

	def getTimetableForCourse(courseCode: String) =
		Futures.flatten(services
			.collect { case service: CourseTimetableFetchingService => service }
			.map {
				// On downstream failures, just return Nil
				_.getTimetableForCourse(courseCode).recover { case t => logger.warn("Error fetching timetable", t); Nil }
			}
		).map(mergeDuplicates)

	def getTimetableForStaff(universityId: String) =
		Futures.flatten(services
			.collect { case service: StaffTimetableFetchingService => service }
			.map {
				// On downstream failures, just return Nil
				_.getTimetableForStaff(universityId).recover { case t => logger.warn("Error fetching timetable", t); Nil }
			}
		).map(mergeDuplicates)

	def getTimetableForRoom(roomName: String) =
		Futures.flatten(services
			.collect { case service: RoomTimetableFetchingService => service }
			.map {
				// On downstream failures, just return Nil
				_.getTimetableForRoom(roomName).recover { case t => logger.warn("Error fetching timetable", t); Nil }
			}
		).map(mergeDuplicates)
}