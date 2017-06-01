package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.feedback.{GenerateGradesFromMarkCommand, PublishFeedbackCommand}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/publish"))
class PublishFeedbackController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, user: CurrentUser): PublishFeedbackCommand.Command =
		PublishFeedbackCommand(mandatory(assignment), user, GenerateGradesFromMarkCommand(mandatory(assignment)))

	@ModelAttribute("isGradeValidation")
	def isGradeValidation(@PathVariable assignment: Assignment): Boolean =
		assignment.module.adminDepartment.assignmentGradeValidation

	@RequestMapping(params = Array("!confirm"))
	def confirmationPage(@ModelAttribute("command") command: PublishFeedbackCommand.Command, @PathVariable assignment: Assignment): Mav =
		Mav("cm2/admin/assignments/publish/form", "gradeValidation" -> command.validateGrades)
			.crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(params = Array("confirm"))
	def publish(@Valid @ModelAttribute("command") command: PublishFeedbackCommand.Command, errors: Errors, @PathVariable assignment: Assignment): Mav =
		if (errors.hasErrors) confirmationPage(command, assignment)
		else {
			command.apply()
			Mav("cm2/admin/assignments/publish/done")
				.crumbsList(Breadcrumbs.assignment(assignment))
		}

}
