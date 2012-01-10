package uk.ac.warwick.courses.web.controllers.admin
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.courses.web.Mav
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.web.Cookies._
import uk.ac.warwick.courses.web.Cookie
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.RequestInfo
import uk.ac.warwick.courses.actions.Masquerade
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.userlookup.UserLookup
import uk.ac.warwick.courses.helpers.FoundUser
import uk.ac.warwick.courses.helpers.NoUser
import org.springframework.web.bind.annotation.ModelAttribute
import collection.JavaConversions._
import java.util.HashMap
import uk.ac.warwick.userlookup.UserLookupInterface

@Controller
@RequestMapping(Array("/admin/masquerade"))
class MasqueradeController extends BaseController {
	
	@Autowired var userLookup:UserLookupInterface =_
	
	private def checkPermissions() = mustBeAbleTo(Masquerade())
	
	//@ModelAttribute def model = new HashMap[String,Object]
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def form():Mav = {
		checkPermissions()
		Mav("sysadmin/masquerade/form")
	}
	
	@RequestMapping(method=Array(RequestMethod.POST),params=Array("!action"))
	def submit(@RequestParam usercode:String, response:HttpServletResponse):Mav = {
		checkPermissions()
		userLookup.getUserByUserId(usercode) match {
			case FoundUser(user) => response addCookie newCookie(usercode)
			case NoUser(user) =>
		}
		Redirect("/admin/masquerade")
	}
	
	@RequestMapping(method=Array(RequestMethod.POST),params=Array("action=remove"))
	def remove(response:HttpServletResponse):Mav = {
		checkPermissions()
		response addCookie newCookie(null)
		Redirect("/admin/masquerade")
	}
	
	private def newCookie(usercode:String) = new Cookie(
					name  = "coursesMasqueradeAs",
					value = usercode,
					path  = "/"
				)
	
}