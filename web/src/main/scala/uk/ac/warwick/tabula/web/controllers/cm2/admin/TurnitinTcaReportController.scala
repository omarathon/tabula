package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.turnitin.TurnitinTcaReportCommand
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("turnitinTca"))
@Controller
@RequestMapping(value = Array(
  "/coursework/admin/assignments/{assignment}/turnitin/tca-report/{attachment}"
))
class TurnitinTcaReportController extends CourseworkController {

  type TurnitinTcaReportCommand = TurnitinTcaReportCommand.Command

  @ModelAttribute("command") def command(
    @PathVariable assignment: Assignment,
    @PathVariable attachment: FileAttachment,
    user: CurrentUser
  ): TurnitinTcaReportCommand = TurnitinTcaReportCommand(mandatory(assignment), mandatory(attachment), user)

  @RequestMapping
  def goToReport(@ModelAttribute("command") command: TurnitinTcaReportCommand): Mav = {
    command.apply().fold(
      error => Mav("cm2/admin/assignments/turnitin/report_error", "problem" -> error),
      uri => Mav("redirect:" + uri.toString)
    )
  }
}