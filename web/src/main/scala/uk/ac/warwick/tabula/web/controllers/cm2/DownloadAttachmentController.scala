package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.DownloadAttachmentCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.fileserver.{ContentDisposition, RenderableFile}
import uk.ac.warwick.tabula.services.{ProfileService, SubmissionService}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/coursework/submission/{assignment}"))
class DownloadAttachmentController extends CourseworkController {

  var submissionService: SubmissionService = Wire.auto[SubmissionService]

  @ModelAttribute def command(@PathVariable assignment: Assignment, user: CurrentUser): Appliable[Option[RenderableFile]] =
    new DownloadAttachmentCommand(assignment, mandatory(submissionService.getSubmissionByUsercode(assignment, user.userId)), optionalCurrentMember)

  @RequestMapping(value = Array("/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def getAttachment(command: Appliable[Option[RenderableFile]], user: CurrentUser): RenderableFile =
    command.apply().map(_.withContentDisposition(ContentDisposition.Attachment))
      .getOrElse(throw new ItemNotFoundException)

}


@Controller
@RequestMapping(value = Array("/coursework/submission/{assignment}/{student}"))
class DownloadAttachmentForStudentController extends CourseworkController {

  var submissionService: SubmissionService = Wire[SubmissionService]
  var profileService: ProfileService = Wire.auto[ProfileService]

  @ModelAttribute def command(@PathVariable assignment: Assignment, @PathVariable student: User): Appliable[Option[RenderableFile]] = {
    val studentMember = profileService.getMemberByUniversityIdStaleOrFresh(student.getWarwickId)
    new DownloadAttachmentCommand(assignment, mandatory(submissionService.getSubmissionByUsercode(assignment, student.getUserId)), studentMember)
  }

  @RequestMapping(value = Array("/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def getAttachment(command: Appliable[Option[RenderableFile]], user: CurrentUser): RenderableFile = {
    command.apply().map(_.withContentDisposition(ContentDisposition.Attachment))
      .getOrElse(throw new ItemNotFoundException)
  }

}
