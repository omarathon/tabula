package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.Features
import org.springframework.beans.factory.annotation.Autowired
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
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import javax.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.services.MaintenanceModeMessage

class MaintenanceModeForm(service: MaintenanceModeService) extends SelfValidating {
	@BeanProperty var enable: Boolean = service.enabled

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var until: DateTime = service.until getOrElse DateTime.now.plusMinutes(30)

	@BeanProperty var message: String = service.message orNull

	def validate(errors: Errors) {

	}
}

@Controller
@RequestMapping(Array("/sysadmin/maintenance"))
class MaintenanceModeController extends BaseSysadminController {
	var service = Wire.auto[MaintenanceModeService]
	var queue = Wire.named[Queue]("settingsSyncTopic")

	validatesSelf[MaintenanceModeForm]

	@ModelAttribute def cmd = new MaintenanceModeForm(service)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(form: MaintenanceModeForm, errors: Errors) =
		Mav("sysadmin/maintenance").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@Valid form: MaintenanceModeForm, errors: Errors) = {
		if (errors.hasErrors)
			showForm(form, errors)
		else {
			if (!form.enable) {
				form.message = null
				form.until = null
			}
			service.message = Option(form.message)
			service.until = Option(form.until)
			if (form.enable) service.enable
			else service.disable
			
			queue.send(new MaintenanceModeMessage(service))

			Redirect("/sysadmin")
		}
	}

}