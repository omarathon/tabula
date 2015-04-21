package uk.ac.warwick.tabula.profiles.web.controllers

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.timetables.ViewModuleTimetableCommandFactoryImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.profiles.commands.{DepartmentTimetablesCommandRequest, DepartmentTimetablesCommand}
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.timetables.{AutowiringScientiaConfigurationComponent, ScientiaHttpTimetableFetchingService}
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.JavaImports._

@Controller
@RequestMapping(Array("/department/{department}/timetables"))
class DepartmentTimetablesController extends ProfilesController
	with AutowiringScientiaConfigurationComponent with SystemClockComponent
	with AutowiringUserLookupComponent {

	val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)

	@ModelAttribute("command")
	def command(@PathVariable department: Department) = {
		DepartmentTimetablesCommand(
			department,
			new ViewModuleTimetableCommandFactoryImpl(timetableFetchingService)
		)
	}

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[EventOccurrence]], @PathVariable department: Department) = {
		Mav("timetables/department")
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("command") cmd: Appliable[Seq[EventOccurrence]] with DepartmentTimetablesCommandRequest,
		@PathVariable department: Department
	) = {
		val timetableEvents = cmd.apply()
		val calendarEvents = timetableEvents.map(FullCalendarEvent(_, userLookup))
		Mav(new JSONView(calendarEvents))
	}

}
