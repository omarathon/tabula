package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.api.commands.profiles.UsercodeSearchCommand
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/usercodeSearch"))
class UsercodeSearchController extends ApiController with AutowiringProfileServiceComponent {

	final override def onPreRequest {
		session.enableFilter(Member.ActiveOnlyFilter)
		session.enableFilter(Member.FreshOnlyFilter)
	}

	@ModelAttribute("getCommand")
	def getCommand: Appliable[Seq[String]] = UsercodeSearchCommand()

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def search(
		@ModelAttribute("getCommand") command: Appliable[Seq[String]],
		@RequestParam(required = false) level: String
	): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"userCodes" -> command.apply()
		)))
	}

}
