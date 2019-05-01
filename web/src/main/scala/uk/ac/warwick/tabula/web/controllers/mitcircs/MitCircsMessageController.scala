package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.mitcircs.{ListMessagesCommand, SendMessageCommand, SendMessageState}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/profiles/view/{student}/personalcircs/mitcircs/view/{submission}/messages"))
class MitCircsMessageController extends BaseController {

  validatesSelf[SelfValidating]
  implicit val defaultOrderingForDateTime: Ordering[DateTime] = Ordering.by[DateTime, Long](_.getMillis)

  type MessageCommand = Appliable[MitigatingCircumstancesMessage] with SendMessageState with SelfValidating

  @ModelAttribute("listCommand")
  def listCommand(
    @PathVariable student: StudentMember,
    @PathVariable submission: MitigatingCircumstancesSubmission
  ): Appliable[Seq[MitigatingCircumstancesMessage]] = {
    mustBeLinked(mandatory(submission), mandatory(student))
    ListMessagesCommand(mandatory(submission))
  }

  @ModelAttribute("messageCommand")
  def messageCommand(@PathVariable submission: MitigatingCircumstancesSubmission): MessageCommand = {
    SendMessageCommand(mandatory(submission), user.apparentUser)
  }

  @RequestMapping
  def form(
    @ModelAttribute("messageCommand") cmd: MessageCommand,
    @ModelAttribute("listCommand") listCmd: Appliable[Seq[MitigatingCircumstancesMessage]],
    @PathVariable submission: MitigatingCircumstancesSubmission
  ): Mav = {
    val messages = listCmd.apply()
    Mav("mitcircs/messages",
      "messages" -> messages,
      "latestMessage" -> messages.map(_.createdDate).max
    ).noLayout()
  }

  @RequestMapping(method = Array(POST))
  def message(
    @Valid @ModelAttribute("messageCommand") cmd: MessageCommand,
    errors: Errors,
    @ModelAttribute("listCommand") listCmd: Appliable[Seq[MitigatingCircumstancesMessage]],
    @PathVariable submission: MitigatingCircumstancesSubmission
  ): Mav = {
    if (errors.hasErrors) form(cmd, listCmd, submission)
    else {
      cmd.apply()
      RedirectForce(Routes.Profile.PersonalCircumstances.messages(submission))
    }
  }

}
