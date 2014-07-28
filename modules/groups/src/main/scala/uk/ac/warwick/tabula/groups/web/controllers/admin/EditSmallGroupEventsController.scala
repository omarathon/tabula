package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange, SmallGroupSet}
import uk.ac.warwick.tabula.groups.commands.admin.EditSmallGroupEventsCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.JavaImports._

abstract class AbstractEditSmallGroupEventsController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditSmallGroupEventsCommand = Appliable[SmallGroupSet]

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupEventsCommand =
		EditSmallGroupEventsCommand(module, set)

	@ModelAttribute("allDays") def allDays = DayOfWeek.members

	case class TermWeekRange(val weekRange: WeekRange) {
		def isFull(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.forall(weeks.contains(_))
		def isPartial(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.exists(weeks.contains(_))
	}

	@ModelAttribute("allTermWeekRanges") def allTermWeekRanges(@PathVariable("smallGroupSet") set: SmallGroupSet) = {
		WeekRange.termWeekRanges(Option(set.academicYear).getOrElse(AcademicYear.guessByDate(DateTime.now)))
			.map { TermWeekRange(_) }
	}

	protected def renderPath: String

	protected def render(set: SmallGroupSet) = {
		Mav(renderPath).crumbs(Breadcrumbs.Department(set.module.department), Breadcrumbs.Module(set.module))
	}

	@RequestMapping
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupEventsCommand
	) = render(set)

	@RequestMapping(method = Array(POST), params=Array("action!=refresh"))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			Redirect(Routes.admin.module(set.module))
		}
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}/events"))
@Controller
class CreateSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "admin/groups/newevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents, "action!=refresh"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups, "action!=refresh"))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate, "action!=refresh"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAllocate(set))
		}
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/events"))
@Controller
class EditSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "admin/groups/editevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents, "action!=refresh"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups, "action!=refresh"))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate, "action!=refresh"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAllocate(set))
		}
	}

}
