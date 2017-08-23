package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.commands.cm2.{StudentMemberSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommand}
import uk.ac.warwick.tabula.data.model.{Assignment, Member}
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/submission/{assignment}/{studentMember}"))
class AssignmentInformationForStudentController extends CourseworkController {

	type StudentSubmissionAndFeedbackCommand = Appliable[StudentSubmissionInformation] with StudentMemberSubmissionAndFeedbackCommandState


	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable studentMember: Member): StudentSubmissionAndFeedbackCommand =
		StudentSubmissionAndFeedbackCommand(assignment, studentMember, user)

	@RequestMapping
	def assignmentGadgetInStudentProfile(@ModelAttribute("command") command: StudentSubmissionAndFeedbackCommand): Mav = {
		val info = command.apply()

		Mav(
			"cm2/submit/assignment",
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
			.withTitle(command.assignment.module.name + " (" + command.assignment.module.code.toUpperCase + ")" + " - " + command.assignment.name)
	}

}