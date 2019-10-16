package uk.ac.warwick.tabula.web.controllers.profiles.admin.timetables

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.profiles.admin.timetables.TimetableCheckerCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/admin/timetablechecker"))
class TimetableCheckerController extends ProfilesController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(): TimetableCheckerCommand.Command = TimetableCheckerCommand()

  @RequestMapping
  def showForm(): Mav = Mav("profiles/admin/timetablechecker")

  @PostMapping
  def submit(@Valid @ModelAttribute("command") command: TimetableCheckerCommand.Command, errors: Errors): Mav =
    if (errors.hasErrors) {
      showForm()
    } else {
      val results = command.apply()

      Mav("profiles/admin/timetablechecker_results",
        "syllabusPlusFeed" -> results.syllabusPlusFeed,
        "wbsFeed" -> results.wbsFeed
      )
    }

}
