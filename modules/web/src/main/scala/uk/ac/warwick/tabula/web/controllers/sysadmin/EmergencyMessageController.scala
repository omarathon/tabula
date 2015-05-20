package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, SelfValidating, Unaudited}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{EmergencyMessage, EmergencyMessageService}
import uk.ac.warwick.util.queue.Queue

class EmergencyMessageCommand(service: EmergencyMessageService) extends Command[Unit] with ReadOnly with Unaudited with SelfValidating {

	PermissionCheck(Permissions.ManageEmergencyMessage)

	val DefaultMaintenanceMinutes = 30

	var queue = Wire.named[Queue]("settingsSyncTopic")

	var enable: Boolean = service.enabled
	var message: String = service.message.orNull

	def applyInternal() {
		if (!enable) {
			message = null
		}
		service.message = Option(message)
		if (enable) service.enable
		else service.disable

		queue.send(new EmergencyMessage(service))
	}

	def validate(errors: Errors) {

	}
}

@Controller
@RequestMapping(Array("/sysadmin/emergencymessage"))
class EmergencyMessageController extends BaseSysadminController {

	var service = Wire.auto[EmergencyMessageService]

	validatesSelf[MaintenanceModeCommand]

	@ModelAttribute def cmd = new EmergencyMessageCommand(service)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(form: EmergencyMessageCommand, errors: Errors) =
		Mav("sysadmin/emergency-message").crumbs(Breadcrumbs.Current("Sysadmin emergency message"))
			.noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@Valid form: EmergencyMessageCommand, errors: Errors) = {
		if (errors.hasErrors)
			showForm(form, errors)
		else {
			form.apply()
			Redirect("/sysadmin")
		}
	}
}
