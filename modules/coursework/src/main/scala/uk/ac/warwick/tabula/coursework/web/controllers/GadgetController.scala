package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.sso.client.SSOConfiguration

@Controller
class GadgetController extends CourseworkController {

	@RequestMapping(value = Array("/api/gadget.xml"))
	def xml = Mav("gadgets/xml",
		"oauthScope" -> SSOConfiguration.getConfig.getString("shire.providerid")).xml() // xml() removes layout and sets xml content type

}
