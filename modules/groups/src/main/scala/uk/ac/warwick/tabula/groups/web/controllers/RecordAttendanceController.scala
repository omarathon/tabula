package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.groups.commands.{RecordAttendanceState, AddAdditionalStudent, RecordAttendanceCommand, SmallGroupEventInFutureCheck}
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ RequestMapping, PathVariable, ModelAttribute }
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventAttendance
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.PopulateOnForm
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState

@RequestMapping(value = Array("/event/{event}/register"))
@Controller
class RecordAttendanceController extends GroupsController {

	validatesSelf[SelfValidating]
	
	type RecordAttendanceCommand = Appliable[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])] 
								   with PopulateOnForm with SmallGroupEventInFutureCheck with RecordAttendanceState with AddAdditionalStudent

	@ModelAttribute
	def command(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, user: CurrentUser)
		: RecordAttendanceCommand
			= RecordAttendanceCommand(event, week, user)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		command.populate()
		form(command)
	}

	def form(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		Mav("groups/attendance/form",
			"command" -> command,
			"allCheckpointStates" -> AttendanceState.values,
			"eventInFuture" -> command.isFutureEvent,
			"returnTo" -> getReturnTo(Routes.tutor.mygroups))
	}

	@RequestMapping(method = Array(POST), params = Array("action=refresh"))
	def refresh(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		command.addAdditionalStudent(command.members)

		form(command)
	}

	@RequestMapping(method = Array(POST), params = Array("action!=refresh"))
	def submit(@Valid @ModelAttribute command: RecordAttendanceCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(command)
		} else {
			val (occurrence, _) = command.apply()
			Redirect(Routes.tutor.mygroups, "updatedOccurrence" -> occurrence.id)
		}
	}

}
