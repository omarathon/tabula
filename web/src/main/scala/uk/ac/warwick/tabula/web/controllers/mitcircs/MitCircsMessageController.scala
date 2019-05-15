package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{GetMapping, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.{ListMessagesCommand, RenderMitCircsMessageAttachmentCommand, SendMessageCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/messages"))
class MitCircsMessageController extends BaseController {

  validatesSelf[SelfValidating]

  type MessageCommand = SendMessageCommand.Command

  @ModelAttribute("listCommand")
  def listCommand(@PathVariable submission: MitigatingCircumstancesSubmission): Appliable[Seq[MitigatingCircumstancesMessage]] =
    ListMessagesCommand(mandatory(submission))

  @ModelAttribute("messageCommand")
  def messageCommand(@PathVariable submission: MitigatingCircumstancesSubmission): MessageCommand = {
    SendMessageCommand(mandatory(submission), user.apparentUser)
  }

  @RequestMapping
  def form(
    @ModelAttribute("messageCommand") cmd: MessageCommand,
    @ModelAttribute("listCommand") listCmd: Appliable[Seq[MitigatingCircumstancesMessage]],
    @PathVariable submission: MitigatingCircumstancesSubmission,
    user: CurrentUser
  ): Mav = {
    val messages = listCmd.apply()
    Mav("mitcircs/messages",
      "messages" -> messages,
      "studentView" -> (user.apparentUser.getWarwickId == submission.student.universityId),
      "latestMessage" -> messages.sortBy(_.createdDate).lastOption.map(_.createdDate)
    ).noLayout()
  }

  @RequestMapping(method = Array(POST))
  def message(
    @Valid @ModelAttribute("messageCommand") cmd: MessageCommand,
    errors: Errors,
    @ModelAttribute("listCommand") listCmd: Appliable[Seq[MitigatingCircumstancesMessage]],
    @PathVariable submission: MitigatingCircumstancesSubmission,
    user: CurrentUser
  ): Mav = {
    if (errors.hasErrors) form(cmd, listCmd, submission, user)
    else {
      cmd.apply()
      RedirectForce(Routes.Messages(submission))
    }
  }

}


@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/messages/{message}/supporting-file/{filename}"))
class MitCircsMessageAttachmentController extends BaseController {

  type RenderAttachmentCommand = Appliable[Option[RenderableAttachment]]

  @ModelAttribute("renderAttachmentCommand")
  def attachmentCommand(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable message: MitigatingCircumstancesMessage,
    @PathVariable filename: String
  ): RenderAttachmentCommand = {
    mustBeLinked(message, submission)
    RenderMitCircsMessageAttachmentCommand(mandatory(message), mandatory(filename))
  }

  @GetMapping
  def supportingFile(@ModelAttribute("renderAttachmentCommand") attachmentCommand: RenderAttachmentCommand, @PathVariable filename: String): RenderableFile =
    attachmentCommand.apply().getOrElse(throw new ItemNotFoundException())

}
