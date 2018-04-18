package uk.ac.warwick.tabula.web.controllers.profiles.admin.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.profiles.admin.timetables.TimetableCheckerCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/admin/timetablechecker"))
class TimetableCheckerController extends ProfilesController {

	@ModelAttribute("command")
	def command(): TimetableCheckerCommand.Command = TimetableCheckerCommand()

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute("command") cmd: TimetableCheckerCommand.Command): Mav = {
		Mav("profiles/admin/timetablechecker")
	}

	@RequestMapping(method=Array(POST))
	def submit(@ModelAttribute("command") command: TimetableCheckerCommand.Command): Mav = {
		val wbsFeed = command.apply()

		Mav("profiles/admin/timetablechecker_results",
			"academicYear" -> AcademicYear.now().getLabel.replace("/", ""),
			"wbsFeed" -> wbsFeed
		)
	}
}