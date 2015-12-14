package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, SelfValidating, Unaudited}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, MaintenanceModeMessage}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.util.queue.Queue

class MaintenanceModeCommand extends Command[Unit]
	with ReadOnly with Unaudited
	with SelfValidating
	with AutowiringMaintenanceModeServiceComponent {

	PermissionCheck(Permissions.ManageMaintenanceMode)

	val DefaultMaintenanceMinutes = 30

	var queue = Wire.named[Queue]("settingsSyncTopic")

	var enable: Boolean = maintenanceModeService.enabled

	@WithinYears(maxFuture = 1, maxPast = 1) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var until: DateTime = maintenanceModeService.until.getOrElse(DateTime.now.plusMinutes(DefaultMaintenanceMinutes))

	var message: String = maintenanceModeService.message.orNull

	def applyInternal() {
		if (!enable) {
			message = null
			until = null
		}
		maintenanceModeService.message = Option(message)
		maintenanceModeService.until = Option(until)
		if (enable) maintenanceModeService.enable
		else maintenanceModeService.disable

		queue.send(new MaintenanceModeMessage(enable, Option(until), Option(message)))
	}

	def validate(errors: Errors) {

	}
}

@Controller
@RequestMapping(Array("/sysadmin/maintenance"))
class MaintenanceModeController extends BaseSysadminController {
	validatesSelf[MaintenanceModeCommand]

	@ModelAttribute def cmd = new MaintenanceModeCommand

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