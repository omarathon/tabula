package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.mitcircs.ListMessagesCommand
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/admin/{department}/view/{submission}/messages"))
class MitCircsMessageController extends BaseController {

  @ModelAttribute("listCommand")
  def listCommand(@PathVariable submission: MitigatingCircumstancesSubmission): Appliable[Seq[MitigatingCircumstancesMessage]] = {
    ListMessagesCommand(mandatory(submission))
  }

//  @ModelAttribute("messageCommand")
//  def messageCommand(@PathVariable submission: MitigatingCircumstancesSubmission): Appliable[MitigatingCircumstancesMessage] = {
//    ??? //CreateMessageCommand(mandatory(submission))
//  }

  @RequestMapping
  def render(
    @ModelAttribute("listCommand") cmd: Appliable[Seq[MitigatingCircumstancesMessage]],
    @PathVariable submission: MitigatingCircumstancesSubmission
  ): Mav = {
    Mav("mitcircs/messages", "messages" -> cmd.apply())
  }

//  @RequestMapping(method = Array(POST))
//  def message(
//    @ModelAttribute("messageCommand") cmd: Appliable[MitigatingCircumstancesMessage],
//    @PathVariable submission: MitigatingCircumstancesSubmission
//  ): Mav = {
//    ???
//  }

}
