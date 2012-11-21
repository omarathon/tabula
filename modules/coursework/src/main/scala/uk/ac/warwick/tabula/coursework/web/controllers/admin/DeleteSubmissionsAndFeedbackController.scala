package uk.ac.warwick.tabula.web.controllers.admin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.actions.Delete
import collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.coursework.commands.assignments.DeleteSubmissionsAndFeedbackCommand
import uk.ac.warwick.tabula.web.Breadcrumbs
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/delete"))
class DeleteSubmissionsAndFeedback extends CourseworkController {
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
