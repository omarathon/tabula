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
import uk.ac.warwick.tabula.services.{TermService, AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.util.termdates.Term

trait SmallGroupEventsController extends GroupsController {
	self: TermServiceComponent =>

	validatesSelf[SelfValidating]

	@ModelAttribute("allDays") def allDays = DayOfWeek.members

	case class NamedTerm(val name: String, val term: Term, val weekRange: WeekRange)

	@ModelAttribute("allTerms") def allTerms(@PathVariable("smallGroupSet") set: SmallGroupSet) = {
		val year = Option(set.academicYear).getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		val weeks = termService.getAcademicWeeksForYear(year.dateInTermOne).toMap

		val terms =
			weeks
				.map { case (weekNumber, dates) =>
					(weekNumber, termService.getTermFromAcademicWeekIncludingVacations(weekNumber, year))
				}
				.groupBy { _._2 }
				.map { case (term, weekNumbersAndTerms) =>
					(term, WeekRange(weekNumbersAndTerms.keys.min, weekNumbersAndTerms.keys.max))
				}
				.toSeq
				.sortBy { case (_, weekRange) => weekRange.minWeek.toInt }

		TermService.orderedTermNames.zip(terms).map { case (name, (term, weekRange)) => NamedTerm(name, term, weekRange) }
	}
}

abstract class AbstractCreateSmallGroupEventController extends SmallGroupEventsController with AutowiringTermServiceComponent {

	type CreateSmallGroupEventCommand = Appliable[SmallGroupEvent] with ModifySmallGroupEventCommandState

	@ModelAttribute("createSmallGroupEventCommand") def cmd(
		@PathVariable("module") module: Module,
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@PathVariable("smallGroup") group: SmallGroup
	): CreateSmallGroupEventCommand =
		ModifySmallGroupEventCommand.create(module, set, group)

	protected def cancelUrl(set: SmallGroupSet): String

	@RequestMapping
	def form(@ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand) = {
		Mav("admin/groups/events/new", "cancelUrl" -> cancelUrl(cmd.set)).crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
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

	override def cancelUrl(set: SmallGroupSet) = Routes.admin.createAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.createAddEvents(set))

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/new"))
@Controller
class EditSmallGroupSetCreateEventController extends AbstractCreateSmallGroupEventController {

	override def cancelUrl(set: SmallGroupSet) = Routes.admin.editAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.editAddEvents(set))

}

abstract class AbstractEditSmallGroupEventController extends SmallGroupEventsController with AutowiringTermServiceComponent {

	type EditSmallGroupEventCommand = Appliable[SmallGroupEvent] with ModifySmallGroupEventCommandState

	@ModelAttribute("editSmallGroupEventCommand") def cmd(
		@PathVariable("module") module: Module,
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@PathVariable("smallGroup") group: SmallGroup,
		@PathVariable("smallGroupEvent") event: SmallGroupEvent
	): EditSmallGroupEventCommand =
		ModifySmallGroupEventCommand.edit(module, set, group, event)

	protected def cancelUrl(set: SmallGroupSet): String

	@RequestMapping
	def form(@ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand) = {
		Mav("admin/groups/events/edit", "cancelUrl" -> cancelUrl(cmd.set)).crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
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

	override def cancelUrl(set: SmallGroupSet) = Routes.admin.createAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.createAddEvents(set))

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}"))
@Controller
class EditSmallGroupSetEditEventController extends AbstractEditSmallGroupEventController {

	override def cancelUrl(set: SmallGroupSet) = Routes.admin.editAddEvents(set)

	@RequestMapping(method = Array(POST))
	def saveAndExit(@Valid @ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		submit(cmd, errors, Routes.admin.editAddEvents(set))

}