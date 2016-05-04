package uk.ac.warwick.tabula.api.web.controllers.groups


import javax.validation.Valid

import org.joda.time.LocalTime
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.groups.SmallGroupEventController.{DeleteSmallGroupEventCommand, SmallGroupEventCommand}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

import scala.beans.BeanProperty


object SmallGroupEventController {
	type DeleteSmallGroupEventCommand = Appliable[SmallGroupEvent] with DeleteSmallGroupEventCommandState with DeleteSmallGroupEventValidation
	type SmallGroupEventCommand = ModifySmallGroupEventCommand.Command
}


@Controller
@RequestMapping(Array("/v1/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/new"))
class CreateSmallGroupEventControllerForApi extends SmallGroupSetController with CreateSmallGroupEventApi

trait CreateSmallGroupEventApi {
	self: SmallGroupSetController =>

	@ModelAttribute("command")
	def createCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup): SmallGroupEventCommand =
		ModifySmallGroupEventCommand.create(module, smallGroupSet, smallGroup)


	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def createEvent(@RequestBody request: SmallGroupEventRequest, @ModelAttribute("command") command: SmallGroupEventCommand, errors: Errors, @PathVariable smallGroupSet: SmallGroupSet) = {
		request.copyTo(command, errors)
		globalValidator.validate(command, errors)
		command.validate(errors)
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			getSmallGroupSetMav(smallGroupSet)
		}
	}
}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}"))
class EditSmallGroupEventControllerForApi extends SmallGroupSetController with EditSmallGroupEventApi

trait EditSmallGroupEventApi {
	self: SmallGroupSetController =>

	@ModelAttribute("editCommand")
	def editCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup, @PathVariable smallGroupEvent: SmallGroupEvent): SmallGroupEventCommand =
		ModifySmallGroupEventCommand.edit(module, smallGroupSet, smallGroup, smallGroupEvent)


	@RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def editEvent(@RequestBody request: SmallGroupEventRequest, @ModelAttribute("editCommand") command: SmallGroupEventCommand, errors: Errors, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroupEvent: SmallGroupEvent) = {
		request.copyTo(command, errors)
		globalValidator.validate(command, errors)
		command.validate(errors)
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			getSmallGroupSetMav(smallGroupSet)
		}
	}
}


@Controller
@RequestMapping(Array("/v1/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}"))
class DeleteSmallGroupEventControllerForApi extends SmallGroupSetController with DeleteSmallGroupEventApi

trait DeleteSmallGroupEventApi {
	self: SmallGroupSetController =>

	@ModelAttribute("deleteCommand")
	def deleteCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup, @PathVariable smallGroupEvent: SmallGroupEvent): DeleteSmallGroupEventCommand =
		DeleteSmallGroupEventCommand(smallGroup, smallGroupEvent)


	@RequestMapping(method = Array(DELETE), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def deleteEvent(@Valid @ModelAttribute("deleteCommand") command: DeleteSmallGroupEventCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok"
			)))
		}
	}
}


class SmallGroupEventRequest extends JsonApiRequest[SmallGroupEventCommand] {
	@BeanProperty var title: String = _
	@BeanProperty var tutors: JList[String] = _
	@BeanProperty var weeks: JSet[JInteger] = _
	@BeanProperty var day: DayOfWeek = null
	@BeanProperty var startTime: LocalTime = _
	@BeanProperty var endTime: LocalTime = _
	@BeanProperty var location: String = _

	override def copyTo(state: SmallGroupEventCommand, errors: Errors) {
		Option(title).foreach {
			state.title = _
		}
		Option(tutors).foreach {
			state.tutors = _
		}
		Option(weeks).foreach {
			state.weeks = _
		}
		Option(day).foreach {
			state.day = _
		}
		Option(startTime).foreach {
			state.startTime = _
		}
		Option(endTime).foreach {
			state.endTime = _
		}
		Option(location).foreach {
			state.location = _
		}
	}
}
