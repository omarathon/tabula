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

@Controller
@RequestMapping(Array("/admin/masquerade"))
class MasqueradeController extends BaseController {

	@Autowired var userLookup: UserLookupInterface = _

	private def checkPermissions() = mustBeAbleTo(Masquerade())

	//@ModelAttribute def model = new HashMap[String,Object]

	@RequestMapping(method = Array(HEAD, GET))
	def form(): Mav = {
		checkPermissions()
		Mav("sysadmin/masquerade/form")
	}

	@RequestMapping(method = Array(POST), params = Array("!action"))
	def submit(@RequestParam usercode: String, response: HttpServletResponse): Mav = {
		checkPermissions()
		userLookup.getUserByUserId(usercode) match {
			case FoundUser(user) => response addCookie newCookie(usercode)
			case NoUser(user) =>
		}
		Redirect("/admin/masquerade")
	}

	@RequestMapping(method = Array(POST), params = Array("action=remove"))
	def remove(response: HttpServletResponse): Mav = {
		checkPermissions()
		response addCookie newCookie(null)
		Redirect("/admin/masquerade")
	}

	private def newCookie(usercode: String) = new Cookie(
		name = "tabulaMasqueradeAs",
		value = usercode,
		path = "/")

}