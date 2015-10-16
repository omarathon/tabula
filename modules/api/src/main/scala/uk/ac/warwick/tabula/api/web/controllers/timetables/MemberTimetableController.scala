package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.{CurrentUser, RequestFailedException}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.TimetableEventToJsonConverter
import uk.ac.warwick.tabula.commands.timetables.{ViewMemberTimetableCommand, ViewMemberTimetableRequest}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.timetables.TimetableEvent

import MemberTimetableController._
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}

import scala.util.{Failure, Success, Try}

object MemberTimetableController {
	type ViewMemberTimetableCommand = Appliable[Try[Seq[TimetableEvent]]] with ViewMemberTimetableRequest with SelfValidating
}

@Controller
@RequestMapping(Array("/v1/member/{member}/timetable"))
class MemberTimetableController extends ApiController
	with GetMemberTimetableApi
	with TimetableEventToJsonConverter

trait GetMemberTimetableApi {
	self: ApiController with TimetableEventToJsonConverter =>

	validatesSelf[SelfValidating]

	@ModelAttribute("getTimetableCommand")
	def command(@PathVariable member: Member, currentUser: CurrentUser): ViewMemberTimetableCommand =
		ViewMemberTimetableCommand(member, currentUser)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def showModuleTimetable(@Valid @ModelAttribute("getTimetableCommand") command: ViewMemberTimetableCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else command.apply() match {
			case Success(events) => Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"events" -> events.map(jsonTimetableEventObject)
			)))
			case Failure(t) => throw new RequestFailedException("The timetabling service could not be reached", t)
		}
	}
}
