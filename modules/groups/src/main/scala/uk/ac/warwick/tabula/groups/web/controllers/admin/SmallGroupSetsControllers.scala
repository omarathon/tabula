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

trait SmallGroupSetsController extends GroupsController {
	
	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	
	@ModelAttribute("allFormats") def allFormats =
		SmallGroupFormat.members.toSeq
		
	@ModelAttribute("module") def module(@PathVariable("module") module: Module) = module 
	
	override final def binding[A](binder: WebDataBinder, cmd: A) {
		binder.registerCustomEditor(classOf[SmallGroupFormat], new AbstractPropertyEditor[SmallGroupFormat] {
			override def fromString(code: String) = SmallGroupFormat.fromCode(code)			
			override def toString(format: SmallGroupFormat) = format.code
		})
	}
	
}

@RequestMapping(Array("/admin/module/{module}/groups/new"))
@Controller
class CreateSmallGroupSetController extends SmallGroupSetsController {
	
	validatesSelf[CreateSmallGroupSetCommand]
	
	@ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module) = 
		new CreateSmallGroupSetCommand(module)
	
	@RequestMapping
	def form(cmd: CreateSmallGroupSetCommand) =
		Mav("admin/groups/new").crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	
	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: CreateSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
}

@RequestMapping(Array("/admin/module/{module}/groups/{set}/edit"))
@Controller
class EditSmallGroupSetController extends SmallGroupSetsController {
	
	validatesSelf[EditSmallGroupSetCommand]
		
	@ModelAttribute("smallGroupSet") def set(@PathVariable("set") set: SmallGroupSet) = set 
	
	@ModelAttribute("editSmallGroupSetCommand") def cmd(@PathVariable("set") set: SmallGroupSet) = 
		new EditSmallGroupSetCommand(set)
	
	@RequestMapping
	def form(cmd: EditSmallGroupSetCommand) =
		Mav("admin/groups/edit").crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	
	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: EditSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
}