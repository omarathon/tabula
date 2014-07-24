package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{RequestParam, InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.groups.commands.admin._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import org.springframework.validation.BeanPropertyBindingResult
import uk.ac.warwick.tabula.commands._
import scala.collection.JavaConverters._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSetSelfSignUpState
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewModule, ViewSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

trait SmallGroupSetsController extends GroupsController {

	validatesSelf[SelfValidating]

	var smallGroupService = Wire[SmallGroupService]

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters
	
	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessByDate(DateTime.now).yearsSurrounding(2, 2)
	
	@ModelAttribute("allFormats") def allFormats = SmallGroupFormat.members

	@ModelAttribute("departmentSmallGroupSets") def departmentSmallGroupSets(@PathVariable("module") module: Module) =
		smallGroupService.getDepartmentSmallGroupSets(module.department)
	
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

@RequestMapping(Array("/admin/module/{module}/groups/new"))
@Controller
class CreateSmallGroupSetController extends SmallGroupSetsController {
	
	type CreateSmallGroupSetCommand = Appliable[SmallGroupSet] with CreateSmallGroupSetCommandState
	
	@ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module): CreateSmallGroupSetCommand =
		ModifySmallGroupSetCommand.create(module)

	@RequestMapping
	def form(@ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand) = {
		Mav("admin/groups/new").crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents))
	def submitAndAddStudents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.createAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups))
	def submitAndAddGroups(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.createAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents))
	def submitAndAddEvents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.createAddEvents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate))
	def submitAndAllocate(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.createAllocate(set))
		}
	}
}

@RequestMapping(Array("/admin/module/{module}/groups/{smallGroupSet}/edit", "/admin/module/{module}/groups/edit/{smallGroupSet}"))
@Controller
class EditSmallGroupSetController extends SmallGroupSetsController {
	
	type EditSmallGroupSetCommand = Appliable[SmallGroupSet] with EditSmallGroupSetCommandState
	
	@ModelAttribute("editSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		ModifySmallGroupSetCommand.edit(module, set)

	@RequestMapping
	def form(@ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand) = {
		Mav("admin/groups/edit").crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents))
	def submitAndAddStudents(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.editAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups))
	def submitAndAddGroups(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.editAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents))
	def submitAndAddEvents(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.editAddEvents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate))
	def submitAndAllocate(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.editAllocate(set))
		}
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













