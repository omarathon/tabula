package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.groups.commands._
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
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
import scala.collection.JavaConverters._

@RequestMapping(value = Array("/event/{event}/register"))
@Controller
class RecordAttendanceController extends GroupsController with AutowiringSmallGroupServiceComponent {

	validatesSelf[SelfValidating]
	
	type RecordAttendanceCommand = Appliable[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])] 
								   with PopulateOnForm with SmallGroupEventInFutureCheck with RecordAttendanceState
									 with AddAdditionalStudent with RemoveAdditionalStudent

	@ModelAttribute
	def command(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, user: CurrentUser)
		: RecordAttendanceCommand
			= RecordAttendanceCommand(event, week, user)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		command.populate()
		form(command)
	}

	def form(command: RecordAttendanceCommand): Mav = {
		Mav("groups/attendance/form",
			"command" -> command,
			"allCheckpointStates" -> AttendanceState.values,
			"eventInFuture" -> command.isFutureEvent,
			"returnTo" -> getReturnTo(Routes.tutor.mygroups))
	}

	@RequestMapping(method = Array(POST), params = Array("action=refresh"))
	def refresh(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		command.addAdditionalStudent(command.members)
		command.doRemoveAdditionalStudent(command.members)

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

	@RequestMapping(value = Array("/additional"))
	def addAdditionalForm(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, @RequestParam student: Member) = {
		case class EventOccurrenceAndState(
			event: SmallGroupEvent,
			week: Int,
			occurrence: Option[SmallGroupEventOccurrence],
			attendance: Option[SmallGroupEventAttendance]
		)

		// Find any groups that the student SHOULD be attending in the same module
		val possibleReplacements: Seq[EventOccurrenceAndState] =
			smallGroupService.findSmallGroupsByStudent(student.asSsoUser)
				.filter { _.groupSet.module == event.group.groupSet.module }
				.filterNot { _ == event.group } // No self references!
				.flatMap { group =>
					val occurrences = smallGroupService.findAttendanceByGroup(group)

					group.events.filter { !_.isUnscheduled }.flatMap { event =>
						val allWeeks = event.weekRanges.flatMap { _.toWeeks }
						allWeeks.map { week =>
							val occurrence = occurrences.find { o =>
								o.event == event && o.week == week
							}

							EventOccurrenceAndState(
								event,
								week,
								occurrence,
								occurrence.flatMap { _.attendance.asScala.find { _.universityId == student.universityId }}
							)
						}
					}
				}
				.filterNot { // Can't replace an attended instance
					_.attendance.exists { _.state == AttendanceState.Attended }
				}

		Mav("groups/attendance/additional",
			"student" -> student,
			"possibleReplacements" -> possibleReplacements,
			"week" -> week
		).noLayout()
	}

}