package uk.ac.warwick.courses.web.controllers
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import javax.validation.Valid
import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.commands.assignments._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import java.io.OutputStream
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.data.FileDao
import org.springframework.util.FileCopyUtils

/**
 * Screens for department and module admins.
 */

@Controller
class AdminHome extends Controllerism {

	@Autowired var moduleService: ModuleAndDepartmentService = _

	@RequestMapping(Array("/admin/"))
	def homeScreen(user: CurrentUser) = {
		Mav("admin/home",
			"ownedDepartments" -> moduleService.departmentsOwnedBy(user.idForPermissions))
	}

	@RequestMapping(Array("/admin/department/{dept}/"))
	def adminDepartment(@PathVariable dept: Department, user: CurrentUser) = {
		mustBeAbleTo(Manage(dept))
		Mav("admin/department",
			"department" -> dept,
			"modules" -> dept.modules.sortBy{ (module) => (module.assignments.isEmpty, module.code) })
	}
	
}
//object AdminControllers {


@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/new"))
class AddAssignment extends Controllerism {

	@ModelAttribute def addAssignmentForm(@PathVariable module: Module) =
		new AddAssignmentCommand(definitely(module))

	@RequestMapping(method = Array(RequestMethod.GET))
	def addAssignmentForm(user: CurrentUser, @PathVariable module: Module,
			form: AddAssignmentCommand, errors: Errors) = {
		mustBeAbleTo(Manage(module))
		Mav("admin/assignments/new",
			"department" -> module.department,
			"module" -> module)
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def addAssignmentSubmit(user: CurrentUser, @PathVariable module: Module,
			@Valid form: AddAssignmentCommand, errors: Errors) = {
		mustBeAbleTo(Manage(module))
		if (errors.hasErrors) {
			addAssignmentForm(user, module, form, errors)
		} else {
			form.apply
			Mav("redirect:/admin/department/" + module.department.code + "/#module-" + module.code)
				
		}
	}

}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/edit/{assignment}"))
class EditAssignment extends Controllerism {
	
	@ModelAttribute def formObject(@PathVariable("assignment") assignment: Assignment) =
		new EditAssignmentCommand(definitely(assignment))
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def showForm(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			form:EditAssignmentCommand, errors: Errors) = {
		
		if (assignment.module != module) throw new ItemNotFoundException
		mustBeAbleTo(Manage(module))
		Mav("admin/assignments/edit",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment
			)
		
	}
	
	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(
			@PathVariable module: Module,
			@PathVariable assignment:Assignment,
			@Valid form: EditAssignmentCommand, errors: Errors) = {
		
		mustBeAbleTo(Manage(module))
		if (errors.hasErrors) {
			showForm(module, assignment, form, errors)
		} else {
			form.apply
			Mav("redirect:/admin/department/" + module.department.code + "/#module-" + module.code)
		}
		
	}
	
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/feedback/{assignment}/download/{feedbackId}/{filename}"))
class DownloadFeedback extends Controllerism {
	@Autowired var fileDao:FileDao =_
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def get(@PathVariable module:Module, @PathVariable assignment:Assignment, @PathVariable feedbackId:String, @PathVariable filename:String, response:HttpServletResponse) {
		fileDao.getFileById(feedbackId) match {
			case Some(file) => {
				//if (file.name != filename) throw new ItemNotFoundException
				response.addHeader("Content-Disposition", "attachment")
				FileCopyUtils.copy(file.dataStream, response.getOutputStream)
			}
			case None => throw new ItemNotFoundException
		}
	}
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/feedback/{assignment}"))
class AddFeedback extends Controllerism {
	
	@ModelAttribute
	def command(@PathVariable assignment:Assignment, user:CurrentUser) = 
		new AddFeedbackCommand(assignment, user)
	
	validatesWith { (cmd:AddFeedbackCommand, errors:Errors) =>
		
	}
	
	def onBind(form:AddFeedbackCommand) {
		form.onBind
	}
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def showForm(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			@ModelAttribute form:AddFeedbackCommand, errors: Errors) = {
		onBind(form)
		if (assignment.module != module) throw new ItemNotFoundException
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/feedback/form",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment
			)
		
	}
	
	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(
			@PathVariable module:Module,
			@PathVariable assignment:Assignment,
			@Valid form:AddFeedbackCommand, errors: Errors) = {
		onBind(form)
		mustBeAbleTo(Participate(module))
//		if (errors.hasErrors) {
			showForm(module, assignment, form, errors)
//		} else {
//			form.apply
//			Mav("redirect:/admin/department/" + module.department.code + "/#module-" + module.code)
//		}
		
	}
	
}


