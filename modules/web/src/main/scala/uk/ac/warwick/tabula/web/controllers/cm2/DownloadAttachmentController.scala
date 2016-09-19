package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.DownloadAttachmentCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Member, Module}
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}"))
class DownloadAttachmentController extends CourseworkController {

	var submissionService = Wire.auto[SubmissionService]

	@ModelAttribute def command(@PathVariable assignment: Assignment, user: CurrentUser): Appliable[Option[RenderableFile]]
		= new DownloadAttachmentCommand(assignment.module, assignment, mandatory(submissionService.getSubmissionByUniId(assignment, user.universityId)), optionalCurrentMember)

	@RequestMapping(value = Array("/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command: Appliable[Option[RenderableFile]], user: CurrentUser): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}/{studentMember}"))
class DownloadAttachmentForStudentController extends CourseworkController {

	var submissionService = Wire[SubmissionService]

	@ModelAttribute def command(@PathVariable assignment: Assignment, @PathVariable studentMember: Member): Appliable[Option[RenderableFile]]
		= new DownloadAttachmentCommand(assignment.module, assignment, mandatory(submissionService.getSubmissionByUniId(assignment, studentMember.universityId)), Some(studentMember))

	@RequestMapping(value = Array("/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command: Appliable[Option[RenderableFile]], user: CurrentUser): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}

}