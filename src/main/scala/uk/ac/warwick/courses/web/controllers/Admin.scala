package uk.ac.warwick.courses.web.controllers

import scala.collection.JavaConversions._
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.commands.assignments._
import uk.ac.warwick.courses.commands.feedback.AdminGetAllFeedbackCommand
import uk.ac.warwick.courses.commands.feedback.AdminGetSingleFeedbackCommand
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.services.fileserver.RenderableAttachment
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.web.Routes
import org.springframework.validation.BeanPropertyBindingResult

/**
 * Screens for department and module admins.
 */

@Controller
class AdminHome extends BaseController {

	@Autowired var moduleService: ModuleAndDepartmentService = _

	@RequestMapping(Array("/admin/"))
	def homeScreen(user: CurrentUser) = {
		Mav("admin/home",
			"ownedDepartments" -> moduleService.departmentsOwnedBy(user.idForPermissions))
	}

	@RequestMapping(Array("/admin/department/{dept}/"))
	def adminDepartment(@PathVariable dept: Department, user: CurrentUser) = {
		val isDeptManager = can(Manage(mandatory(dept)))
		val modules:JList[Module] = if (isDeptManager) {
			dept.modules
		} else {
			moduleService.modulesManagedBy(user.idForPermissions, dept).toList
		}
		if (modules.isEmpty()) {
			mustBeAbleTo(Manage(dept))
		}
		Mav("admin/department",
			"department" -> dept,
			"modules" -> modules.sortBy{ (module) => (module.assignments.isEmpty, module.code) })
			
	}
	
}
//object AdminControllers {


@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/new"))
class AddAssignment extends BaseController {
	
	@Autowired var assignmentService:AssignmentService =_

	@ModelAttribute("academicYearChoices") def academicYearChoices:java.util.List[AcademicYear] = {
		val thisYear = AcademicYear.guessByDate(DateTime.now)
		List(
			thisYear.previous.previous,
			thisYear.previous,
			thisYear,
			thisYear.next,
			thisYear.next.next
		)
	}
	
	validatesWith { (cmd:AddAssignmentCommand, errors:Errors) =>
		cmd.validate(errors)
		
	}
	
	@ModelAttribute def addAssignmentForm(@PathVariable module: Module) =
		new AddAssignmentCommand(mandatory(module))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def addAssignmentForm(user: CurrentUser, @PathVariable module: Module,
			form: AddAssignmentCommand, errors: Errors) = {
		permCheck(module)
		Mav("admin/assignments/new",
			"department" -> module.department,
			"module" -> module)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def addAssignmentSubmit(user: CurrentUser, @PathVariable module: Module,
			@Valid form: AddAssignmentCommand, errors: Errors) = {
		permCheck(module)
		if (errors.hasErrors) {
			addAssignmentForm(user, module, form, errors)
		} else {
			form.apply
			Redirect(Routes.admin.module(module))
		}
	}
	
	def permCheck(module:Module) = mustBeAbleTo(Participate(module)) 

}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/edit"))
class EditAssignment extends BaseController {
	
	validatesWith{ (form:EditAssignmentCommand, errors:Errors) =>
		form.validate(errors)
		if (form.academicYear != form.assignment.academicYear) {
			errors.rejectValue("academicYear", "academicYear.immutable")
		}
	}
	
	@ModelAttribute def formObject(@PathVariable("assignment") assignment: Assignment) =
		new EditAssignmentCommand(mandatory(assignment))
	
	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def showForm(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			form:EditAssignmentCommand, errors: Errors) = {
		
		if (assignment.module != module) throw new ItemNotFoundException
		mustBeAbleTo(Participate(module))
		val couldDelete = canDelete(assignment)
		Mav("admin/assignments/edit",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment,
			"canDelete" -> couldDelete
			)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
	
	private def canDelete(assignment:Assignment):Boolean = {
		val cmd = new DeleteAssignmentCommand(assignment)
		val errors = new BeanPropertyBindingResult(cmd, "cmd")
		cmd.prechecks(errors)
		!errors.hasErrors
	}
	
	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(
			@PathVariable module: Module,
			@PathVariable assignment:Assignment,
			@Valid form: EditAssignmentCommand, errors: Errors) = {
		
		mustBeAbleTo(Participate(module))
		if (errors.hasErrors) {
			showForm(module, assignment, form, errors)
		} else {
			form.apply
			Redirect(Routes.admin.module(module))
		}
		
	}
	
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/delete"))
class DeleteAssignment extends BaseController {
	
	validatesWith{ (form:DeleteAssignmentCommand, errors:Errors) =>
		form.validate(errors)
	}
	
	@ModelAttribute def formObject(@PathVariable("assignment") assignment: Assignment) =
		new DeleteAssignmentCommand(mandatory(assignment))
	
	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def showForm(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			form:DeleteAssignmentCommand, errors: Errors) = {
		
		if (assignment.module != module) throw new ItemNotFoundException
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/delete",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment
			)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
	
	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(
			@PathVariable module: Module,
			@PathVariable assignment:Assignment,
			@Valid form: DeleteAssignmentCommand, errors: Errors) = {
		
		mustBeAbleTo(Participate(module))
		if (errors.hasErrors) {
			showForm(module, assignment, form, errors)
		} else {
			form.apply
			Redirect(Routes.admin.module(module))
		}
		
	}
	
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"))
class DownloadFeedback extends BaseController {
	@Autowired var feedbackDao:FeedbackDao =_
	@Autowired var fileServer:FileServer =_
	
	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(@PathVariable module:Module, @PathVariable assignment:Assignment, @PathVariable feedbackId:String, @PathVariable filename:String, response:HttpServletResponse) {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))

		feedbackDao.getFeedback(feedbackId) match {
			case Some(feedback) => {
				mustBeLinked(feedback, assignment)
				val renderable = new AdminGetSingleFeedbackCommand(feedback).apply()
				fileServer.serve(renderable, response)
			}
			case None => throw new ItemNotFoundException
		}
	}
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/feedback/download-zip/{filename}"))
class DownloadAllFeedback extends BaseController {
	@Autowired var fileServer:FileServer =_
	@RequestMapping
	def download(@PathVariable module:Module, @PathVariable assignment:Assignment, @PathVariable filename:String, response:HttpServletResponse) {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		val renderable = new AdminGetAllFeedbackCommand(assignment).apply()
		fileServer.serve(renderable, response)
	}
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/feedback/list"))
class ListFeedback extends BaseController {
	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(@PathVariable module:Module, @PathVariable assignment:Assignment) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/feedback/list")
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
}




