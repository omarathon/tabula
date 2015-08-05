package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{PopulateOnForm, SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.{CopySmallGroupSetsRequestState, CopySmallGroupSetsCommandState, CopySmallGroupSetsCommand}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

import scala.collection.JavaConverters._

abstract class CopySmallGroupSetsController extends GroupsController {

	validatesSelf[SelfValidating]

	type CopySmallGroupSetsCommand =
		Appliable[Seq[SmallGroupSet]] with PopulateOnForm with CopySmallGroupSetsCommandState with CopySmallGroupSetsRequestState

	private def formView(command: CopySmallGroupSetsCommand) = Mav("admin/groups/copy").crumbsList(crumbsList(command))

	def crumbsList(command: CopySmallGroupSetsCommand): Seq[BreadCrumb]

	@RequestMapping
	def refresh(@ModelAttribute("copySmallGroupSetsCommand") command: CopySmallGroupSetsCommand) = {
		command.populate()
		formView(command)
	}

	@RequestMapping(method = Array(POST), params = Array("action!=refresh"))
	def submit(@Valid @ModelAttribute("copySmallGroupSetsCommand") command: CopySmallGroupSetsCommand, errors: Errors) = {
		if (errors.hasErrors) formView(command)
		else {
			command.apply()
			Redirect(Routes.groups.admin(command.department, command.targetAcademicYear))
		}
	}

	@ModelAttribute("academicYearChoices")
	def academicYearChoices = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(3, 1)

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/groups/copy"))
class CopyModuleSmallGroupSetsController extends CopySmallGroupSetsController {

	@ModelAttribute("copySmallGroupSetsCommand")
	def command(@PathVariable module: Module): CopySmallGroupSetsCommand =
		CopySmallGroupSetsCommand(mandatory(module).adminDepartment, Seq(mandatory(module)))

	override def crumbsList(cmd: CopySmallGroupSetsCommand) =
		Seq(Breadcrumbs.DepartmentForYear(cmd.department, cmd.targetAcademicYear), Breadcrumbs.ModuleForYear(cmd.modules.head, cmd.targetAcademicYear))

}

@Controller
@RequestMapping(value = Array("/admin/department/{department}/groups/copy"))
class CopyDepartmentSmallGroupSetsController extends CopySmallGroupSetsController {

	@ModelAttribute("copySmallGroupSetsCommand")
	def command(@PathVariable department: Department): CopySmallGroupSetsCommand = {
		val modules = mandatory(department).modules.asScala.filter(_.groupSets.asScala.exists { set => !set.deleted && !set.archived }).sortBy { _.code }
		CopySmallGroupSetsCommand(department, modules)
	}

	override def crumbsList(cmd: CopySmallGroupSetsCommand) =
		Seq(Breadcrumbs.DepartmentForYear(cmd.department, cmd.targetAcademicYear))

}
