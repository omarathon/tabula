package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.commands.timetables.{DepartmentTimetablesCommand, ViewModuleTimetableCommandFactoryImpl, ViewStaffPersonalTimetableCommandFactoryImpl, ViewStudentPersonalTimetableCommandFactoryImpl}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{FullCalendarEvent, JSONView}

@Controller
@RequestMapping(Array("/profiles/department/{department}/timetables"))
class DepartmentTimetablesController extends ProfilesController
	with AutowiringScientiaConfigurationComponent with SystemClockComponent
	with AutowiringUserLookupComponent with CurrentSITSAcademicYear {

	val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)

	@ModelAttribute("activeDepartment")
	def activeDepartment(@PathVariable department: Department) = department

	@ModelAttribute("command")
	def command(@PathVariable department: Department): DepartmentTimetablesCommand.CommandType = {
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
	def form(@ModelAttribute("command") cmd: DepartmentTimetablesCommand.CommandType, @PathVariable department: Department) = {
		Mav("profiles/timetables/department",
			"canFilterStudents" -> securityService.can(user, DepartmentTimetablesCommand.FilterStudentPermission, mandatory(department)),
			"canFilterStaff" -> securityService.can(user, DepartmentTimetablesCommand.FilterStaffPermission, mandatory(department))
		)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("command") cmd: DepartmentTimetablesCommand.CommandType,
		@PathVariable department: Department
	) = {
		val result = cmd.apply()
		val calendarEvents = FullCalendarEvent.colourEvents(result._1.events.map(FullCalendarEvent(_, userLookup)))
		Mav(new JSONView(Map("events" -> calendarEvents, "lastUpdated" -> result._1.lastUpdated, "errors" -> result._2)))
	}

}
