package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.DownloadAttachmentCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Member, Module}
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value = Array("/blah/courseworkkkk/module/{module}/{assignment}"))
class DownloadAttachmentController extends OldCourseworkController {

	var submissionService = Wire.auto[SubmissionService]

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser): Appliable[Option[RenderableFile]]
		= new DownloadAttachmentCommand(module, assignment, mandatory(submissionService.getSubmissionByUniId(assignment, user.universityId)), optionalCurrentMember)

	@RequestMapping(value = Array("/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command: Appliable[Option[RenderableFile]], user: CurrentUser): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value = Array("/coursework/module/{module}/{assignment}/{studentMember}"))
class DownloadAttachmentForStudentController extends OldCourseworkController {

	var submissionService = Wire[SubmissionService]

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable studentMember: Member): Appliable[Option[RenderableFile]]
		= new DownloadAttachmentCommand(module, assignment, mandatory(submissionService.getSubmissionByUniId(assignment, studentMember.universityId)), Some(studentMember))

	@RequestMapping(value = Array("/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command: Appliable[Option[RenderableFile]], user: CurrentUser): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}

}