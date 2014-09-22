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
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2)
	
	@ModelAttribute("allFormats") def allFormats = SmallGroupFormat.members
	
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

	@ModelAttribute("departmentSmallGroupSets") def departmentSmallGroupSets(@PathVariable("module") module: Module, @RequestParam(value="academicYear", required=false) academicYear: AcademicYear) =
		smallGroupService.getDepartmentSmallGroupSets(module.department, Option(academicYear).getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))
	
	@ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module): CreateSmallGroupSetCommand =
		ModifySmallGroupSetCommand.create(module)

	@RequestMapping
	def form(@ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand) = {
		Mav("admin/groups/new").crumbs(Breadcrumbs.DepartmentForYear(cmd.module.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
	}

	private def submit(cmd: CreateSmallGroupSetCommand, errors: Errors, route: SmallGroupSet => String) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(route(set))
		}
	}

	@RequestMapping(method = Array(POST), params=Array("action!=refresh"))
	def saveAndExit(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, { _ => Routes.admin(cmd.module.department, cmd.academicYear) })

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents, "action!=refresh"))
	def submitAndAddStudents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAddStudents(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups, "action!=refresh"))
	def submitAndAddGroups(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAddGroups(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents, "action!=refresh"))
	def submitAndAddEvents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAddEvents(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate, "action!=refresh"))
	def submitAndAllocate(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAllocate(_))
}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}"))
@Controller
class CreateSmallGroupSetEditPropertiesController extends SmallGroupSetsController {

	type EditSmallGroupSetCommand = Appliable[SmallGroupSet] with EditSmallGroupSetCommandState

	@ModelAttribute("departmentSmallGroupSets") def departmentSmallGroupSets(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet, @RequestParam(value="academicYear", required=false) academicYear: AcademicYear) =
		smallGroupService.getDepartmentSmallGroupSets(module.department, Option(academicYear).getOrElse(set.academicYear))

	@ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		ModifySmallGroupSetCommand.edit(module, set)

	@RequestMapping
	def form(@ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand) = {
		Mav("admin/groups/new").crumbs(Breadcrumbs.DepartmentForYear(cmd.module.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
	}

	private def submit(cmd: EditSmallGroupSetCommand, errors: Errors, route: SmallGroupSet => String) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(route(set))
		}
	}

	@RequestMapping(method = Array(POST), params=Array("action!=refresh"))
	def saveAndExit(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, { _ => Routes.admin(cmd.module.department, cmd.academicYear) })

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents, "action!=refresh"))
	def submitAndAddStudents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAddStudents(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups, "action!=refresh"))
	def submitAndAddGroups(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAddGroups(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents, "action!=refresh"))
	def submitAndAddEvents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAddEvents(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate, "action!=refresh"))
	def submitAndAllocate(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.createAllocate(_))
}

@RequestMapping(Array("/admin/module/{module}/groups/{smallGroupSet}/edit", "/admin/module/{module}/groups/edit/{smallGroupSet}"))
@Controller
class EditSmallGroupSetController extends SmallGroupSetsController {
	
	type EditSmallGroupSetCommand = Appliable[SmallGroupSet] with EditSmallGroupSetCommandState

	@ModelAttribute("departmentSmallGroupSets") def departmentSmallGroupSets(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet, @RequestParam(value="academicYear", required=false) academicYear: AcademicYear) =
		smallGroupService.getDepartmentSmallGroupSets(module.department, Option(academicYear).getOrElse(set.academicYear))
	
	@ModelAttribute("editSmallGroupSetCommand") def cmd(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet) =
		ModifySmallGroupSetCommand.edit(module, set)

	@RequestMapping
	def form(@ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand) = {
		Mav("admin/groups/edit").crumbs(Breadcrumbs.DepartmentForYear(cmd.module.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
	}

	private def submit(cmd: EditSmallGroupSetCommand, errors: Errors, route: SmallGroupSet => String) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(route(set))
		}
	}

	@RequestMapping(method = Array(POST), params=Array("action!=refresh"))
	def saveAndExit(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, { _ => Routes.admin(cmd.module.department, cmd.academicYear) })

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents, "action!=refresh"))
	def submitAndAddStudents(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.editAddStudents(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups, "action!=refresh"))
	def submitAndAddGroups(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.editAddGroups(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents, "action!=refresh"))
	def submitAndAddEvents(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.editAddEvents(_))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate, "action!=refresh"))
	def submitAndAllocate(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors) =
		submit(cmd, errors, Routes.admin.editAllocate(_))
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
		.crumbs(Breadcrumbs.DepartmentForYear(cmd.module.department, cmd.set.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.set.academicYear))

	@RequestMapping(method = Array(POST))
	def submit(@Valid cmd: DeleteSmallGroupSetCommand, errors: Errors) =
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			Redirect(Routes.admin(cmd.module.department, set.academicYear))
		}
	
}













