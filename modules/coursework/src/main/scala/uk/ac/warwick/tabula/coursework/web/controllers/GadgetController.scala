package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire

@Controller
class GadgetController extends CourseworkController {
	
	// Jump on the back of the home page controller.
	var homeController = Wire.auto[HomeController]

	@RequestMapping(value = Array("/api/gadget.xml"))
	def xml = Mav("gadgets/coursework/xml",
		"oauthScope" -> SSOConfiguration.getConfig.getString("shire.providerid")).xml() // xml() removes layout and sets xml content type

	@RequestMapping(value = Array("/api/gadget.html"))
	def render(user: CurrentUser) = {
		val home = homeController.home(user)
		//home.viewName = "gadgets/coursework/render"
		home.embedded
		home
	}
		
}
