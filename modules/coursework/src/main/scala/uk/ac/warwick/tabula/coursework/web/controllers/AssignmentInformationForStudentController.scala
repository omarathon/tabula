package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.{Member, Assignment, Module}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.{StudentMemberSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommand}
import uk.ac.warwick.tabula.commands.coursework.StudentSubmissionAndFeedbackCommand._

@Controller
@RequestMapping(Array("/module/{module}/{assignment}/{studentMember}"))
class AssignmentInformationForStudentController extends CourseworkController {

	type StudentSubmissionAndFeedbackCommand = Appliable[StudentSubmissionInformation] with StudentMemberSubmissionAndFeedbackCommandState

	@ModelAttribute("command")
	def command(@PathVariable("module") module: Module,
							@PathVariable("assignment") assignment: Assignment,
							@PathVariable("studentMember") studentMember: Member): StudentSubmissionAndFeedbackCommand =
		StudentSubmissionAndFeedbackCommand(module, assignment, studentMember, user)

	@RequestMapping
	def assignmentGadgetInStudentProfile(@ModelAttribute("command") command: StudentSubmissionAndFeedbackCommand) = {
		val info = command.apply()

		Mav(
			"submit/assignment",
			"feedback" -> info.feedback,
			"submission" -> info.submission,
			"justSubmitted" -> false,
			"canSubmit" -> info.canSubmit,
			"canReSubmit" -> info.canReSubmit,
			"hasExtension" -> info.extension.isDefined,
			"hasActiveExtension" -> info.extension.exists(_.approved), // active = has been approved
			"extension" -> info.extension,
			"isExtended" -> info.isExtended,
			"extensionRequested" -> info.extensionRequested,
			"isSelf" -> (user.universityId == command.studentMember.universityId))
			.withTitle(command.module.name + " (" + command.module.code.toUpperCase + ")" + " - " + command.assignment.name)
	}

}