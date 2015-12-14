package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, SelfValidating, Unaudited}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringEmergencyMessageServiceComponent, EmergencyMessage, EmergencyMessageService}
import uk.ac.warwick.util.queue.Queue

class EmergencyMessageCommand extends Command[Unit]
	with ReadOnly with Unaudited
	with SelfValidating
	with AutowiringEmergencyMessageServiceComponent {

	PermissionCheck(Permissions.ManageEmergencyMessage)

	val DefaultMaintenanceMinutes = 30

	var queue = Wire.named[Queue]("settingsSyncTopic")

	var enable: Boolean = emergencyMessageService.enabled
	var message: String = emergencyMessageService.message.orNull

	def applyInternal() {
		if (!enable) {
			message = null
		}
		emergencyMessageService.message = Option(message)
		if (enable) emergencyMessageService.enable
		else emergencyMessageService.disable

		queue.send(new EmergencyMessage(enable, Option(message)))
	}

	def validate(errors: Errors) {

	}
}

@Controller
@RequestMapping(Array("/sysadmin/emergencymessage"))
class EmergencyMessageController extends BaseSysadminController {

	validatesSelf[MaintenanceModeCommand]

	@ModelAttribute def cmd = new EmergencyMessageCommand

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(form: EmergencyMessageCommand, errors: Errors) =
		Mav("sysadmin/emergency-message").noLayoutIf(ajax)

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
