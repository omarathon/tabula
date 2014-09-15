package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.helpers.StringUtils._

trait PartialTimetableFetchingService

trait StudentTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForStudent(universityId: String): Seq[TimetableEvent]
}

trait ModuleTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForModule(moduleCode: String): Seq[TimetableEvent]
}

trait CourseTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForCourse(courseCode: String): Seq[TimetableEvent]
}

trait RoomTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForRoom(roomName: String): Seq[TimetableEvent]
}

trait StaffTimetableFetchingService extends PartialTimetableFetchingService {
	def getTimetableForStaff(universityId: String): Seq[TimetableEvent]
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

class CombinedTimetableFetchingService(services: PartialTimetableFetchingService*) extends CompleteTimetableFetchingService {

	def mergeDuplicates(events: Seq[TimetableEvent]): Seq[TimetableEvent] = {
		// If an event runs on the same day, between the same times, in the same weeks, of the same type, on the same module, it is the same
		events.groupBy { event => (event.year, event.day, event.startTime, event.endTime, event.weekRanges, event.eventType, event.context) }
			.mapValues {
				case event :: Nil => event
				case events => {
					val event = events.head
					TimetableEvent(
						events.flatMap { _.name.maybeText }.headOption.getOrElse(""),
						events.flatMap { _.title.maybeText }.headOption.getOrElse(""),
						events.flatMap { _.description.maybeText }.headOption.getOrElse(""),
						event.eventType,
						event.weekRanges,
						event.day,
						event.startTime,
						event.endTime,
						events.flatMap { _.location }.headOption,
						event.context,
						events.flatMap { _.comments }.headOption,
						events.flatMap { _.staffUniversityIds }.distinct,
						events.flatMap { _.studentUniversityIds }.distinct,
						event.year
					)
				}
			}
			.values.toSeq
	}

	def getTimetableForStudent(universityId: String) =
		mergeDuplicates(services.collect { case service: StudentTimetableFetchingService => service }.flatMap { _.getTimetableForStudent(universityId) })

	def getTimetableForModule(moduleCode: String) =
		mergeDuplicates(services.collect { case service: ModuleTimetableFetchingService => service }.flatMap { _.getTimetableForModule(moduleCode) })

	def getTimetableForCourse(courseCode: String) =
		mergeDuplicates(services.collect { case service: CourseTimetableFetchingService => service }.flatMap { _.getTimetableForCourse(courseCode) })

	def getTimetableForStaff(universityId: String) =
		mergeDuplicates(services.collect { case service: StaffTimetableFetchingService => service }.flatMap { _.getTimetableForStaff(universityId) })

	def getTimetableForRoom(roomName: String) =
		mergeDuplicates(services.collect { case service: RoomTimetableFetchingService => service }.flatMap { _.getTimetableForRoom(roomName) })
}