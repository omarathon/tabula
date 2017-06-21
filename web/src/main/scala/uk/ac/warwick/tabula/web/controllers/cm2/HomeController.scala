package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand.Command
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}"))
class HomeController extends CourseworkController {

	hideDeletedItems

	@ModelAttribute("command")
	def command(user: CurrentUser): Command = {
		CourseworkHomepageCommand(user)
	}

	@RequestMapping
	def home(@ModelAttribute("command") command: Command, user: CurrentUser): Mav = {
		val info = command.apply()

		Mav("cm2/home/view",
			"homeDepartment" -> info.homeDepartment,
			"studentInformation" -> info.studentInformation,
			"markerInformation" -> info.markerInformation,
			"adminInformation" -> info.adminInformation
		)
	}

}
@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/{academicYear:\\d{4}}", "/${cm2.prefix}/admin", "/${cm2.prefix}/admin/department", "/${cm2.prefix}/submission", "/${cm2.prefix}/module/**"))
class HomeRewritesController extends BaseController {

	@RequestMapping
	def rewriteToHome = Redirect(Routes.home)

}