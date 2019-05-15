package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.mitcircs.{AddMitCircSubmissionNoteCommand, DeleteMitCircSubmissionNoteCommand, ListMitCircSubmissionNotesCommand, RenderMitCircsNoteAttachmentCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesNote, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/notes"))
class MitCircsNotesController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("listCommand")
  def listCommand(@PathVariable submission: MitigatingCircumstancesSubmission): ListMitCircSubmissionNotesCommand.Command =
    ListMitCircSubmissionNotesCommand(submission)

  @ModelAttribute("addCommand")
  def addCommand(@PathVariable submission: MitigatingCircumstancesSubmission, user: CurrentUser): AddMitCircSubmissionNoteCommand.Command =
    AddMitCircSubmissionNoteCommand(submission, user.apparentUser)

  @RequestMapping
  def list(@ModelAttribute("listCommand") listCommand: ListMitCircSubmissionNotesCommand.Command): Mav =
    Mav("mitcircs/notes", "notes" -> listCommand.apply()).noLayout()

  @PostMapping
  def addNote(
    @Valid @ModelAttribute("addCommand") command: AddMitCircSubmissionNoteCommand.Command,
    errors: Errors,
    @ModelAttribute("listCommand") listCommand: ListMitCircSubmissionNotesCommand.Command,
    @PathVariable submission: MitigatingCircumstancesSubmission,
  ): Mav =
    if (errors.hasErrors) list(listCommand)
    else {
      command.apply()
      RedirectForce(Routes.Admin.review(submission))
    }

}

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/notes/{note}/delete"))
class DeleteMitCircsNoteController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("deleteCommand")
  def deleteCommand(@PathVariable submission: MitigatingCircumstancesSubmission, @PathVariable note: MitigatingCircumstancesNote): DeleteMitCircSubmissionNoteCommand.Command =
    DeleteMitCircSubmissionNoteCommand(submission, note)

  @PostMapping
  def deleteNote(
    @Valid @ModelAttribute("deleteCommand") command: DeleteMitCircSubmissionNoteCommand.Command,
    errors: Errors,
    @PathVariable submission: MitigatingCircumstancesSubmission,
  ): Mav =
    if (errors.hasErrors) RedirectForce(Routes.Admin.review(submission))
    else {
      command.apply()
      RedirectForce(Routes.Admin.review(submission))
    }

}

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/notes/{note}/supporting-file/{filename}"))
class MitCircsNoteAttachmentController extends BaseController {

  type RenderAttachmentCommand = Appliable[Option[RenderableAttachment]]

  @ModelAttribute("renderAttachmentCommand")
  def attachmentCommand(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable note: MitigatingCircumstancesNote,
    @PathVariable filename: String
  ): RenderAttachmentCommand = {
    mustBeLinked(note, submission)
    RenderMitCircsNoteAttachmentCommand(mandatory(note), mandatory(filename))
  }

  @RequestMapping(method = Array(GET))
  def supportingFile(@ModelAttribute("renderAttachmentCommand") attachmentCommand: RenderAttachmentCommand, @PathVariable filename: String): RenderableFile =
    attachmentCommand.apply().getOrElse(throw new ItemNotFoundException())

}
