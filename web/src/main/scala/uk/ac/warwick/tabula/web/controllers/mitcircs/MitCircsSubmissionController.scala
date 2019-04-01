package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.mitcircs.submission._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/profile/{student}/new"))
class MitCircsSubmissionController extends BaseController {

  validatesSelf[SelfValidating]

  type CreateCommand = Appliable[MitigatingCircumstancesSubmission] with CreateMitCircsSubmissionState with SelfValidating

  @ModelAttribute("createMitCircsCommand") def create(@PathVariable student: StudentMember, user: CurrentUser): CreateCommand =
    CreateMitCircsSubmissionCommand(student, user.apparentUser)

  @RequestMapping
  def form(@ModelAttribute("createMitCircsCommand") cmd: CreateCommand, @PathVariable student: StudentMember): Mav = {
    Mav("mitcircs/submissions/create", Map("issueTypes" -> IssueType.values))
  }

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("createMitCircsCommand") cmd: CreateCommand, errors: Errors, @PathVariable student: StudentMember): Mav = {
    if (errors.hasErrors) form(cmd, student)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Student.home(student))
    }
  }
}

@Controller
@RequestMapping(value = Array("/mitcircs/profile/{student}/edit/{submission}"))
class MitCircsEditController extends BaseController {

  validatesSelf[SelfValidating]
  type EditCommand = Appliable[MitigatingCircumstancesSubmission] with EditMitCircsSubmissionState with SelfValidating

  @ModelAttribute("editMitCircsCommand") def edit(@PathVariable submission: MitigatingCircumstancesSubmission, user: CurrentUser): EditCommand =
    EditMitCircsSubmissionCommand(mandatory(submission), user.apparentUser)

  @RequestMapping
  def form(@ModelAttribute("editMitCircsCommand") cmd: EditCommand, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    Mav("mitcircs/submissions/edit", Map("issueTypes" -> IssueType.values))
  }

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("editMitCircsCommand") cmd: EditCommand, errors: Errors, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    if (errors.hasErrors) form(cmd, submission)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Student.home(submission.student))
    }
  }

}

@Controller
@RequestMapping(Array("/mitcircs/profile/{student}/{submission}/supporting-file/{filename}"))
class MitCircsAttachmentController extends BaseController {

  type RenderAttachmentCommand = Appliable[Option[RenderableAttachment]]

  @ModelAttribute("renderAttachmentCommand")
  def attachmentCommand(@PathVariable submission: MitigatingCircumstancesSubmission, @PathVariable filename: String) =
    RenderMitCircsAttachmentCommand(mandatory(submission), mandatory(filename))

  @RequestMapping(method = Array(GET))
  def supportingFile(@ModelAttribute("renderAttachmentCommand") attachmentCommand: RenderAttachmentCommand, @PathVariable filename: String): RenderableFile =
    attachmentCommand.apply().getOrElse(throw new ItemNotFoundException())

}

