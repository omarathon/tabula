package uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.EmailNotificationService

@Controller
@RequestMapping(Array("/sysadmin/emails", "/sysadmin/emails/list"))
class EmailQueueController extends BaseController {

	var emailNotificationService = Wire[EmailNotificationService]

	val pageSize = 100

	@ModelAttribute("emails")
	def emails(@RequestParam(value = "page", defaultValue = "1") page: Int) = {
		val start = (page * pageSize) + 1
		val max = pageSize

		emailNotificationService.recentRecipients(start, max)
	}

	@RequestMapping def list(@RequestParam(value = "page", defaultValue = "1") page: Int) =
		Mav("sysadmin/emails/list", "page" -> page)

}
