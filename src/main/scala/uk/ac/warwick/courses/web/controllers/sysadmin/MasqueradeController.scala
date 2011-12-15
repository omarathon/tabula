package uk.ac.warwick.courses.web.controllers.sysadmin
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.courses.web.Mav
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.web.Cookies._
import uk.ac.warwick.courses.web.Cookie

@Controller
@RequestMapping(Array("/sysadmin/masquerade"))
class MasqueradeController extends BaseSysadminController {
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def form = Mav("sysadmin/masquerade/form")
	
	@RequestMapping(method=Array(RequestMethod.POST),params=Array("!action"))
	def submit(@RequestParam usercode:String, response:HttpServletResponse) = {
		response.addCookie(new Cookie(
			name  = "coursesMasqueradeAs", 
			value = usercode,
			path  = "/"
		))
		Mav("redirect:/sysadmin/masquerade")
	}
	
	@RequestMapping(method=Array(RequestMethod.POST),params=Array("action=remove"))
	def remove(response:HttpServletResponse) = submit(null, response)
	
}