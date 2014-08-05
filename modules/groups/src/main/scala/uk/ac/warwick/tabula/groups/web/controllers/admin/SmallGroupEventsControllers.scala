package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.groups.commands.admin.{ModifySmallGroupEventCommandState, ModifySmallGroupEventCommand}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController

trait SmallGroupEventsController extends GroupsController {

	validatesSelf[SelfValidating]

	@ModelAttribute("allDays") def allDays = DayOfWeek.members

	case class TermWeekRange(val weekRange: WeekRange) {
		def isFull(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.forall(weeks.contains(_))
		def isPartial(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.exists(weeks.contains(_))
	}

	@ModelAttribute("allTermWeekRanges") def allTermWeekRanges(@PathVariable("smallGroupSet") set: SmallGroupSet) = {
		WeekRange.termWeekRanges(Option(set.academicYear).getOrElse(AcademicYear.guessByDate(DateTime.now)))
			.map { TermWeekRange(_) }
	}
}

abstract class AbstractCreateSmallGroupEventController extends SmallGroupEventsController {

	type CreateSmallGroupEventCommand = Appliable[SmallGroupEvent] with ModifySmallGroupEventCommandState

	@ModelAttribute("createSmallGroupEventCommand") def cmd(
		@PathVariable("module") module: Module,
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@PathVariable("smallGroup") group: SmallGroup
	): CreateSmallGroupEventCommand =
		ModifySmallGroupEventCommand.create(module, set, group)

	@RequestMapping
	def form(@ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand) = {
		Mav("admin/groups/events/new").crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	}

	protected def submit(cmd: CreateSmallGroupEventCommand, errors: Errors, route: String) = {
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			RedirectForce(route)
		}
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}/events/{smallGroup}/new"))
@Controller
class CreateSmallGroupSetCreateEventController extends AbstractCreateSmallGroupEventController {

	@ModelAttribute("cancelUrl") def cancelUrl(@PathVariable("smallGroupSet") set: SmallGroupSet) =
		Routes.admin.createAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.createAddEvents(set))

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/new"))
@Controller
class EditSmallGroupSetCreateEventController extends AbstractCreateSmallGroupEventController {

	@ModelAttribute("cancelUrl") def cancelUrl(@PathVariable("smallGroupSet") set: SmallGroupSet) =
		Routes.admin.editAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.editAddEvents(set))

}

abstract class AbstractEditSmallGroupEventController extends SmallGroupEventsController {

	type EditSmallGroupEventCommand = Appliable[SmallGroupEvent] with ModifySmallGroupEventCommandState

	@ModelAttribute("editSmallGroupEventCommand") def cmd(
		@PathVariable("module") module: Module,
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@PathVariable("smallGroup") group: SmallGroup,
		@PathVariable("smallGroupEvent") event: SmallGroupEvent
	): EditSmallGroupEventCommand =
		ModifySmallGroupEventCommand.edit(module, set, group, event)

	@RequestMapping
	def form(@ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand) = {
		Mav("admin/groups/events/edit").crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	}

	protected def submit(cmd: EditSmallGroupEventCommand, errors: Errors, route: String) = {
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			RedirectForce(route)
		}
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}"))
@Controller
class CreateSmallGroupSetEditEventController extends AbstractEditSmallGroupEventController {

	@ModelAttribute("cancelUrl") def cancelUrl(@PathVariable("smallGroupSet") set: SmallGroupSet) =
		Routes.admin.createAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.createAddEvents(set))

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}"))
@Controller
class EditSmallGroupSetEditEventController extends AbstractEditSmallGroupEventController {

	@ModelAttribute("cancelUrl") def cancelUrl(@PathVariable("smallGroupSet") set: SmallGroupSet) =
		Routes.admin.editAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.editAddEvents(set))

}