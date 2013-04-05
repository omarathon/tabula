package uk.ac.warwick.tabula.home.web.controllers.admin

import scala.collection.JavaConversions._

import org.hibernate.validator.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod._

import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.home.commands.admin.MasqueradeCommand
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/admin/masquerade"))
class MasqueradeController extends BaseController {

	@ModelAttribute("masqueradeCommand") def command = new MasqueradeCommand()

	@RequestMapping(method = Array(HEAD, GET))
	def form(@ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand): Mav = Mav("sysadmin/masquerade/form")

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand, response: HttpServletResponse): Mav = {
		for (cookie <- cmd.apply()) response.addCookie(cookie)
		Redirect("/admin/masquerade")
	}

}