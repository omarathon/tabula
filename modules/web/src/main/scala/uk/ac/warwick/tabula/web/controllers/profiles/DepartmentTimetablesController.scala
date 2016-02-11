package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.timetables.{ViewStudentPersonalTimetableCommandFactoryImpl, ViewStaffPersonalTimetableCommandFactoryImpl, ViewModuleTimetableCommandFactoryImpl}
import uk.ac.warwick.tabula.commands.{Appliable, CurrentSITSAcademicYear}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.commands.timetables.{DepartmentTimetablesCommand, DepartmentTimetablesCommandRequest}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.web.views.{FullCalendarEvent, JSONView}

@Controller
@RequestMapping(Array("/profiles/department/{department}/timetables"))
class DepartmentTimetablesController extends ProfilesController
	with AutowiringScientiaConfigurationComponent with SystemClockComponent
	with AutowiringUserLookupComponent with CurrentSITSAcademicYear {

	val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)

	@ModelAttribute("command")
	def command(@PathVariable department: Department) = {
		DepartmentTimetablesCommand(
			mandatory(department),
			academicYear,
			user,
			new ViewModuleTimetableCommandFactoryImpl(timetableFetchingService),
			new ViewStudentPersonalTimetableCommandFactoryImpl(user),
			new ViewStaffPersonalTimetableCommandFactoryImpl(user)
		)
	}

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[(Seq[EventOccurrence], Seq[String])], @PathVariable department: Department) = {
		Mav("profiles/timetables/department")
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
