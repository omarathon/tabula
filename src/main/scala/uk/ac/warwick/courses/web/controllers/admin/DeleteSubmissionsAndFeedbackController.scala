package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.commands.assignments.DeleteSubmissionCommand
import org.springframework.transaction.annotation.Transactional
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Delete
import collection.JavaConversions._
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.commands.assignments.DeleteSubmissionCommand
import uk.ac.warwick.courses.commands.assignments.DeleteSubmissionsAndFeedbackCommand
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.data.FeedbackDao

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/delete"))
class DeleteSubmissionsAndFeedback extends BaseController {
    @Autowired var assignmentService: AssignmentService = _
	@Autowired var feedbackDao: FeedbackDao = _

	@ModelAttribute
	def command(@PathVariable assignment: Assignment) = new DeleteSubmissionsAndFeedbackCommand(assignment)

	validatesSelf[DeleteSubmissionsAndFeedbackCommand]

	def formView(assignment: Assignment) = Mav("admin/assignments/submissionsandfeedback/delete",
		"assignment" -> assignment)
		.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	@RequestMapping(method = Array(GET))
	def get(@PathVariable("assignment") assignment: Assignment) = RedirectBack(assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment,
		form: DeleteSubmissionsAndFeedbackCommand, errors: Errors) = {
		mustBeLinked(assignment, module)

		for (
			uniId <- form.students;
			submission <- assignmentService.getSubmissionByUniId(assignment, uniId)
		) {
		    mustBeAbleTo(Delete(mandatory(submission)))
		}
		
        for (
            uniId <- form.students;
            feedback <- feedbackDao.getFeedbackByUniId(assignment, uniId)
        ) {
        	mustBeAbleTo(Delete(mandatory(feedback)))
        }
		
		form.prevalidate(errors)
		formView(assignment)
	}

	@Transactional
	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@Valid form: DeleteSubmissionsAndFeedbackCommand,
		errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		if (errors.hasErrors) {
			formView(assignment)
		} else {
			form.apply()
			RedirectBack(assignment)
		}
	}
}
