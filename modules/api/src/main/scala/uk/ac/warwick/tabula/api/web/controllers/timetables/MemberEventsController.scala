package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.{CurrentUser, RequestFailedException}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.EventOccurrenceToJsonConverter
import uk.ac.warwick.tabula.commands.timetables.{ViewMemberEventsCommand, ViewMemberEventsRequest}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.timetables.EventOccurrence

import MemberEventsController._
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}

import scala.util.{Failure, Success, Try}

object MemberEventsController {
	type ViewMemberEventsCommand = Appliable[Try[Seq[EventOccurrence]]] with ViewMemberEventsRequest with SelfValidating
}

@Controller
@RequestMapping(Array("/v1/member/{member}/timetable/events"))
class MemberEventsController extends ApiController
	with GetMemberEventsApi
	with EventOccurrenceToJsonConverter

trait GetMemberEventsApi {
	self: ApiController with EventOccurrenceToJsonConverter =>

	validatesSelf[SelfValidating]

	@ModelAttribute("getTimetableCommand")
	def command(@PathVariable member: Member, currentUser: CurrentUser): ViewMemberEventsCommand =
		ViewMemberEventsCommand(member, currentUser)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def showModuleTimetable(@Valid @ModelAttribute("getTimetableCommand") command: ViewMemberEventsCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else command.apply() match {
			case Success(events) => Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"events" -> events.map(jsonEventOccurrenceObject)
			)))
			case Failure(t) => throw new RequestFailedException("The timetabling service could not be reached", t)
		}
	}
}
