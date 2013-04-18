package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.Features

import org.springframework.stereotype.Controller
import org.springframework.beans.BeanWrapperImpl
import collection.JavaConversions._
import java.beans.PropertyDescriptor
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import scala.annotation.target.field
import scala.annotation.target.param
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.BeanWrapper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.tabula.FeaturesMessage
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.joda.time.DateTime
import javax.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.services.MaintenanceModeMessage
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.permissions._

class MaintenanceModeCommand(service: MaintenanceModeService) extends Command[Unit] with ReadOnly with Unaudited with SelfValidating {
	
	PermissionCheck(Permissions.ManageMaintenanceMode)
	
	var queue = Wire[Queue]("settingsSyncTopic")
	
	var enable: Boolean = service.enabled

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var until: DateTime = service.until getOrElse DateTime.now.plusMinutes(30)

	var message: String = service.message.orNull
	
	def applyInternal() {
		if (!enable) {
			message = null
			until = null
		}
		service.message = Option(message)
		service.until = Option(until)
		if (enable) service.enable
		else service.disable
		
		queue.send(new MaintenanceModeMessage(service))
	}

	def validate(errors: Errors) {

	}
}

@Controller
@RequestMapping(Array("/sysadmin/maintenance"))
class MaintenanceModeController extends BaseSysadminController {
	var service = Wire[MaintenanceModeService]

	validatesSelf[MaintenanceModeCommand]

	@ModelAttribute def cmd = new MaintenanceModeCommand(service)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(form: MaintenanceModeCommand, errors: Errors) =
		Mav("sysadmin/maintenance").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@Valid form: MaintenanceModeCommand, errors: Errors) = {
		if (errors.hasErrors)
			showForm(form, errors)
		else {
			form.apply()
			Redirect("/sysadmin")
		}
	}

}