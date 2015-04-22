package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.timetables.ViewModuleTimetableCommandFactoryImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.profiles.commands.{ViewStaffPersonalTimetableCommandFactoryImpl, DepartmentTimetablesCommand, DepartmentTimetablesCommandRequest, ViewStudentPersonalTimetableCommandFactoryImpl}
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/department/{department}/timetables"))
class DepartmentTimetablesController extends ProfilesController
	with AutowiringScientiaConfigurationComponent with SystemClockComponent
	with AutowiringUserLookupComponent {

	val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)

	val studentTimetableEventSource: StudentTimetableEventSource = (new CombinedStudentTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with AutowiringSecurityServiceComponent
		with SystemClockComponent
		).studentTimetableEventSource

	val staffTimetableEventSource: StaffTimetableEventSource = (new CombinedStaffTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with AutowiringSecurityServiceComponent
		with SystemClockComponent
		).staffTimetableEventSource

	val scheduledMeetingEventSource: ScheduledMeetingEventSource = (new MeetingRecordServiceScheduledMeetingEventSourceComponent
		with AutowiringRelationshipServiceComponent
		with AutowiringMeetingRecordServiceComponent
		with AutowiringSecurityServiceComponent
		).scheduledMeetingEventSource

	@ModelAttribute("command")
	def command(@PathVariable department: Department) = {
		DepartmentTimetablesCommand(
			mandatory(department),
			user,
			new ViewModuleTimetableCommandFactoryImpl(timetableFetchingService),
			new ViewStudentPersonalTimetableCommandFactoryImpl(
				studentTimetableEventSource,
				scheduledMeetingEventSource,
				user
			),
			new ViewStaffPersonalTimetableCommandFactoryImpl(
				staffTimetableEventSource,
				scheduledMeetingEventSource,
				user
			)
		)
	}

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[(Seq[EventOccurrence], Seq[String])], @PathVariable department: Department) = {
		Mav("timetables/department")
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("command") cmd: Appliable[(Seq[EventOccurrence], Seq[String])] with DepartmentTimetablesCommandRequest,
		@PathVariable department: Department
	) = {
		val result = cmd.apply()
		val calendarEvents = result._1.map(FullCalendarEvent(_, userLookup))
		Mav(new JSONView(Map("events" -> calendarEvents, "errors" -> result._2)))
	}

}
