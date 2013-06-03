package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.hibernate.validator.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.groups.commands.admin.CreateSmallGroupSetCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.groups.commands.admin.EditSmallGroupSetCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.groups.commands.admin.ModifySmallGroupSetCommand
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.groups.commands.admin.DeleteSmallGroupSetCommand
import org.springframework.validation.BeanPropertyBindingResult
import uk.ac.warwick.tabula.groups.commands.admin.ArchiveSmallGroupSetCommand

trait SmallGroupSetsController extends GroupsController {
	
	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	
	@ModelAttribute("allFormats") def allFormats = SmallGroupFormat.members
	
	@ModelAttribute("allDays") def allDays = DayOfWeek.members
		
	@ModelAttribute("module") def module(@PathVariable("module") module: Module) = module 
	
	def allTermWeekRanges(cmd: ModifySmallGroupSetCommand) = {
		WeekRange.termWeekRanges(Option(cmd.academicYear).getOrElse(AcademicYear.guessByDate(DateTime.now)))
		.map { TermWeekRange(_) }
	}
	
	override final def binding[A](binder: WebDataBinder, cmd: A) {		
		binder.registerCustomEditor(classOf[SmallGroupFormat], new AbstractPropertyEditor[SmallGroupFormat] {
			override def fromString(code: String) = SmallGroupFormat.fromCode(code)			
			override def toString(format: SmallGroupFormat) = format.code
		})
		binder.registerCustomEditor(classOf[SmallGroupAllocationMethod], new AbstractPropertyEditor[SmallGroupAllocationMethod] {
			override def fromString(code: String) = SmallGroupAllocationMethod.fromDatabase(code)			
			override def toString(method: SmallGroupAllocationMethod) = method.dbValue
		})
	}
	
}

case class TermWeekRange(val weekRange: WeekRange) {
	def isFull(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.forall(weeks.contains(_))
	def isPartial(weeks: JList[WeekRange.Week]) = weekRange.toWeeks.exists(weeks.contains(_))
}

@RequestMapping(Array("/admin/module/{module}/groups/new"))
@Controller
class CreateSmallGroupSetController extends SmallGroupSetsController {
	
	validatesSelf[CreateSmallGroupSetCommand]
	
	@ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module) = 
		new CreateSmallGroupSetCommand(module)
		
	@RequestMapping
	def form(cmd: CreateSmallGroupSetCommand) =
		Mav("admin/groups/new",
			"allTermWeekRanges" -> allTermWeekRanges(cmd)
		).crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	
	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: CreateSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			
			// Redirect straight to allocation
			Redirect(Routes.admin.allocate(set))
		}
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/edit"))
@Controller
class EditSmallGroupSetController extends SmallGroupSetsController {
	
	validatesSelf[EditSmallGroupSetCommand]
		
	@ModelAttribute("smallGroupSet") def set(@PathVariable("set") set: SmallGroupSet) = set 
	
	@ModelAttribute("editSmallGroupSetCommand") def cmd(@PathVariable("set") set: SmallGroupSet) = 
		new EditSmallGroupSetCommand(set)

	@ModelAttribute("canDelete") def canDelete(@PathVariable("set") set: SmallGroupSet) = {
		val cmd = new DeleteSmallGroupSetCommand(set.module, set)
		val errors = new BeanPropertyBindingResult(cmd, "cmd")
		cmd.validateCanDelete(errors)
		!errors.hasErrors
	}
	
	@RequestMapping
	def form(cmd: EditSmallGroupSetCommand) =
		Mav("admin/groups/edit",
			"allTermWeekRanges" -> allTermWeekRanges(cmd)
		).crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	
	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: EditSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/delete"))
@Controller
class DeleteSmallGroupSetController extends GroupsController {
	
	validatesSelf[DeleteSmallGroupSetCommand]
	
	@ModelAttribute("smallGroupSet") def set(@PathVariable("set") set: SmallGroupSet) = set 
	
	@ModelAttribute("deleteSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module, @PathVariable("set") set: SmallGroupSet) = 
		new DeleteSmallGroupSetCommand(module, set)

	@RequestMapping
	def form(cmd: DeleteSmallGroupSetCommand) =
		Mav("admin/groups/delete")
		.crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))

	@RequestMapping(method = Array(POST))
	def submit(@Valid cmd: DeleteSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/archive"))
@Controller
class ArchiveSmallGroupSetController extends GroupsController {
		
	@ModelAttribute("smallGroupSet") def set(@PathVariable("set") set: SmallGroupSet) = set 
	
	@ModelAttribute("archiveSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module, @PathVariable("set") set: SmallGroupSet) = 
		new ArchiveSmallGroupSetCommand(module, set)

	@RequestMapping
	def form(cmd: ArchiveSmallGroupSetCommand) =
		Mav("admin/groups/archive").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(cmd: ArchiveSmallGroupSetCommand) = {
		cmd.apply()
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}
	
}