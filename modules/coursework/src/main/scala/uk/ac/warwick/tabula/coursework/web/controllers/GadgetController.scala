package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.tabula.CurrentUser

@Controller
class GadgetController extends CourseworkController {

	@RequestMapping(value = Array("/api/gadget.xml"))
	def xml = Mav("gadgets/coursework/xml",
		"oauthScope" -> SSOConfiguration.getConfig.getString("shire.providerid")).xml() // xml() removes layout and sets xml content type

	@RequestMapping(value = Array("/api/gadget.html"))
	def render(user: CurrentUser) = {
		Mav("gadgets/coursework/render").embedded
	}
		
}
