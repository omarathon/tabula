package uk.ac.warwick.tabula.home.web.controllers.admin

import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.web.Mav
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.Cookie
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.actions.Masquerade
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.userlookup.UserLookup
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.NoUser
import org.springframework.web.bind.annotation.ModelAttribute
import collection.JavaConversions._
import java.util.HashMap
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.tabula.home.commands.admin.MasqueradeCommand
import org.hibernate.validator.Valid

@Controller
@RequestMapping(Array("/admin/masquerade"))
class MasqueradeController extends BaseController {
	
	@ModelAttribute("masqueradeCommand") def command = new MasqueradeCommand()

	@RequestMapping(method = Array(HEAD, GET))
	def form(@ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand): Mav = Mav("sysadmin/masquerade/form")

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand, response: HttpServletResponse): Mav = {
		cmd.apply() map { response addCookie _ }
		Redirect("/admin/masquerade")
	}

}