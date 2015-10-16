package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.RequestFailedException
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.EventOccurrenceToJsonConverter
import uk.ac.warwick.tabula.commands.timetables.{ViewModuleEventsCommand, ViewModuleEventsRequest}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.timetables.EventOccurrence

import ModuleEventsController._
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}

import scala.util.{Failure, Success, Try}

object ModuleEventsController {
	type ViewModuleEventsCommand = Appliable[Try[Seq[EventOccurrence]]] with ViewModuleEventsRequest with SelfValidating
}

@Controller
@RequestMapping(Array("/v1/module/{module}/timetable/events"))
class ModuleEventsController extends ApiController
	with GetModuleEventsApi
	with EventOccurrenceToJsonConverter
	with AutowiringProfileServiceComponent

trait GetModuleEventsApi {
	self: ApiController with EventOccurrenceToJsonConverter =>

	validatesSelf[SelfValidating]

	@ModelAttribute("getTimetableCommand")
	def command(@PathVariable module: Module): ViewModuleEventsCommand =
		ViewModuleEventsCommand(module)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def showModuleEvents(@Valid @ModelAttribute("getTimetableCommand") command: ViewModuleEventsCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else command.apply() match {
			case Success(events) => Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"events" -> events.map(jsonEventOccurrenceObject)
			)))
			case Failure(t) => {
				logger.error("Couldn't generate timetable events", t)
				throw new RequestFailedException("The timetabling service could not be reached", t)
			}
		}
	}
}