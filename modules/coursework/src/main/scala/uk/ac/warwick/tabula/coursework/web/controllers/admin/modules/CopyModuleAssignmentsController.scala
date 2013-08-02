package uk.ac.warwick.tabula.coursework.web.controllers.admin.modules

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import scala.Array
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.commands.assignments.CopyAssignmentsCommand
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController

@Controller
@RequestMapping(value = Array("/admin/module/{module}/copy-assignments"))
class CopyModuleAssignmentsController extends CourseworkController {

	@ModelAttribute
	def copyAssignmentsCommand(@PathVariable("module") module: Module) = CopyAssignmentsCommand(Seq(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable("module") module: Module, cmd: CopyAssignmentsCommand) = {
		Mav("admin/modules/copy_assignments",
			"title" -> module.name,
			"cancel" -> Routes.admin.module(module)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: CopyAssignmentsCommand, @PathVariable("module") module: Module, errors: Errors, user: CurrentUser) = {
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}

@Controller
@RequestMapping(value = Array("/admin/department/{department}/copy-assignments"))
class CopyDepartmentAssignmentsController extends CourseworkController {

	@ModelAttribute
	def copyAssignmentsCommand(@PathVariable("department") department: Department) =
		CopyAssignmentsCommand(department.modules.asScala.filter(_.assignments.size > 0))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable("department") department: Department, cmd: CopyAssignmentsCommand) = {
		Mav("admin/modules/copy_assignments",
			"title" -> department.name,
			"cancel" -> Routes.admin.department(department)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: CopyAssignmentsCommand, @PathVariable("department") department: Department, errors: Errors, user: CurrentUser) = {
		cmd.apply()
		Redirect(Routes.admin.department(department))
	}

}
