package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

import org.hibernate.validator.Valid
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping

import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.home.commands.sysadmin.GodModeCommand
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.userlookup.UserLookupInterface

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 * 
 * @deprecated Use version in home module instead
 */

abstract class BaseSysadminController extends BaseController {
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	var userLookup = Wire.auto[UserLookupInterface]

	def redirectToHome = Redirect("/sysadmin/")
}

/* Just a pojo to bind to; actually used in scheduling */
class ReindexForm {
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var from: DateTime = _
}

@Controller
@RequestMapping(Array("/sysadmin"))
class SysadminController extends BaseSysadminController {

	var maintenanceService = Wire.auto[MaintenanceModeService]

	@ModelAttribute("reindexForm") def reindexForm = new ReindexForm

	@RequestMapping
	def home = Mav("sysadmin/home").addObjects("maintenanceModeService" -> maintenanceService)

}

@Controller
@RequestMapping(Array("/sysadmin/god"))
class GodModeController extends BaseSysadminController {
	
	@RequestMapping(method = Array(HEAD, GET))
	def form(cmd: GodModeCommand) = Mav("sysadmin/masquerade/form")

	@RequestMapping(method = Array(POST))
	def submit(@Valid cmd: GodModeCommand, response: HttpServletResponse) = {
		cmd.apply() map { response addCookie _ }
		redirectToHome
	}
	
}
