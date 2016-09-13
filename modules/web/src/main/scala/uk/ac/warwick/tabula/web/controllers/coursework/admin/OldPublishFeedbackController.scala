package uk.ac.warwick.tabula.web.controllers.coursework.admin
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.feedback.{GenerateGradesFromMarkCommand, PublishFeedbackCommand, PublishFeedbackCommandState}
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import javax.validation.Valid

import org.springframework.context.annotation.Profile

@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/publish"))
@Profile(Array("cm1Enabled")) @Controller
class OldPublishFeedbackController extends OldCourseworkController {

	type PublishFeedbackCommand = Appliable[PublishFeedbackCommand.PublishFeedbackResults] with PublishFeedbackCommandState
	validatesSelf[SelfValidating]

	@ModelAttribute("publishFeedbackCommand") def cmd(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser) = {
		PublishFeedbackCommand(mandatory(module), mandatory(assignment), user, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!confirm"))
	def confirmation(@ModelAttribute("publishFeedbackCommand") command: PublishFeedbackCommand, errors: Errors): Mav = {
		if (errors.hasErrors) command.prevalidate(errors)
		Mav(s"$urlPrefix/admin/assignments/publish/form",
			"assignment" -> command.assignment,
			"isGradeValidation" -> command.module.adminDepartment.assignmentGradeValidation,
			"gradeValidation" -> command.validateGrades
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("publishFeedbackCommand") command: PublishFeedbackCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			confirmation(command, errors)
		} else {
			command.apply()
			Mav(s"$urlPrefix/admin/assignments/publish/done", "assignment" -> command.assignment)
		}
	}

}