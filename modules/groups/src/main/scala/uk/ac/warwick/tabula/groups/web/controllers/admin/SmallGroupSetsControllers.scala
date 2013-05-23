package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.groups.commands.admin.CreateSmallGroupSetCommand
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.web.bind.annotation.ModelAttribute
import org.joda.time.DateTime
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.validation.Errors
import org.hibernate.validator.Valid
import uk.ac.warwick.tabula.groups.web.Routes

trait SmallGroupSetsController extends GroupsController {
	
	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
		
	@ModelAttribute("module") def module(@PathVariable("module") module: Module) = module 
	
}

@RequestMapping(Array("/admin/module/{module}/groups/new"))
@Controller
class CreateSmallGroupSetController extends SmallGroupSetsController {
	
	validatesSelf[CreateSmallGroupSetCommand]
	
	@ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module) = 
		new CreateSmallGroupSetCommand(module)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(cmd: CreateSmallGroupSetCommand) =
		Mav("admin/groups/new").crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid cmd: CreateSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
}