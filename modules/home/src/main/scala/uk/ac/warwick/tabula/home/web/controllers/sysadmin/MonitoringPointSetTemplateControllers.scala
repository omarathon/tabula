package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSetTemplate}
import uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates._
import scala.Array
import javax.validation.Valid
import org.springframework.validation.Errors

trait MonitoringPointSetTemplateController extends BaseSysadminController {
	validatesSelf[SelfValidating]
}

@Controller
@RequestMapping(value = Array("/sysadmin/pointsettemplates"))
class ListMonitoringPointSetTemplateController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand() = ListMonitoringPointSetTemplatesCommand()

	@RequestMapping
	def list(@ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSetTemplate]],
					 @RequestParam(value="template", required = false) template: MonitoringPointSetTemplate,
					 @RequestParam(value="method", required = false) method: String) =
		Mav("sysadmin/pointsettemplates/list",
			"templates" -> cmd.apply(),
			"template" -> template,
			"method" -> method
		)
}

@Controller
@RequestMapping(value = Array("/sysadmin/pointsettemplates/reorder"))
class ReorderMonitoringPointSetTemplateController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand() = ReorderMonitoringPointSetTemplatesCommand()

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSetTemplate]], errors: Errors) = {
		cmd.apply()
		Redirect("/sysadmin/pointsettemplates", "method" -> "reorder")
	}
}

@Controller
@RequestMapping(value = Array("/sysadmin/pointsettemplates/add"))
class AddMonitoringPointSetTemplateController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand() = AddMonitoringPointSetTemplateCommand()

	@RequestMapping(method=Array(GET,HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPointSetTemplate]) =
		Mav("sysadmin/pointsettemplates/add_form")

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPointSetTemplate], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			val template = cmd.apply()
			Redirect("/sysadmin/pointsettemplates", "template" -> template.id, "method" -> "add")
		}
	}
}

@Controller
@RequestMapping(value = Array("/sysadmin/pointsettemplates/{template}/edit"))
class EditMonitoringPointSetTemplateController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand(@PathVariable(value = "template") template: MonitoringPointSetTemplate) =
		EditMonitoringPointSetTemplateCommand(template)

	@RequestMapping(method=Array(GET,HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPointSetTemplate]) =
		Mav("sysadmin/pointsettemplates/edit_form")

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPointSetTemplate], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			val template = cmd.apply()
			Redirect("/sysadmin/pointsettemplates", "template" -> template.id, "method" -> "edit")
		}
	}
}

@Controller
@RequestMapping(value = Array("/sysadmin/pointsettemplates/{template}/delete"))
class DeleteMonitoringPointSetTemplateController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand(@PathVariable(value = "template") template: MonitoringPointSetTemplate) =
		DeleteMonitoringPointSetTemplateCommand(template)

	@RequestMapping(method=Array(GET,HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPointSetTemplate]) =
		Mav("sysadmin/pointsettemplates/delete_form")

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPointSetTemplate], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			val template = cmd.apply()
			Redirect("/sysadmin/pointsettemplates", "template" -> template.id, "method" -> "delete")
		}
	}
}

@Controller
@RequestMapping(Array("/sysadmin/pointsettemplates/add/points/add"))
class AddMonitoringPointController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand() = AddMonitoringPointCommand()

	@RequestMapping(method=Array(POST), params = Array("form"))
	def form(@ModelAttribute("command") cmd: Appliable[Unit]) = {
		Mav("sysadmin/pointsettemplates/point/add_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[Unit], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("sysadmin/pointsettemplates/_monitoringPoints").noLayoutIf(ajax)
		}
	}

}

@Controller
@RequestMapping(Array("/sysadmin/pointsettemplates/add/points/edit/{pointIndex}"))
class EditMonitoringPointController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand(@PathVariable pointIndex: Int) = EditMonitoringPointCommand(pointIndex)

	@RequestMapping(method=Array(POST), params = Array("form"))
	def form(@ModelAttribute("command") cmd: Appliable[Unit] with EditMonitoringPointState) = {
		cmd.copyFrom()
		Mav("sysadmin/pointsettemplates/point/edit_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[Unit] with EditMonitoringPointState, errors: Errors) = {
		if (errors.hasErrors) {
			Mav("sysadmin/pointsettemplates/point/edit_form").noLayoutIf(ajax)
		} else {
			cmd.apply()
			Mav("sysadmin/pointsettemplates/_monitoringPoints").noLayoutIf(ajax)
		}
	}

}

@Controller
@RequestMapping(Array("/sysadmin/pointsettemplates/add/points/delete/{pointIndex}"))
class DeleteMonitoringPointController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand(@PathVariable pointIndex: Int) = DeleteMonitoringPointCommand(pointIndex)

	@RequestMapping(method=Array(POST), params = Array("form"))
	def form(@ModelAttribute("command") cmd: Appliable[Unit]) = {
		Mav("sysadmin/pointsettemplates/point/delete_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[Unit], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("sysadmin/pointsettemplates/_monitoringPoints").noLayoutIf(ajax)
		}
	}

}

@Controller
@RequestMapping(Array("/sysadmin/pointsettemplates/{template}/edit/points/add"))
class CreateMonitoringPointController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand(@PathVariable template: MonitoringPointSetTemplate) = CreateMonitoringPointCommand(template)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint]) = {
		Mav("sysadmin/pointsettemplates/point/create_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("sysadmin/pointsettemplates/_monitoringPointsPersisted").noLayoutIf(ajax)
		}
	}

}

@Controller
@RequestMapping(Array("/sysadmin/pointsettemplates/{template}/edit/points/{point}/edit"))
class UpdateMonitoringPointController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable(value = "template") template: MonitoringPointSetTemplate,
		@PathVariable(value = "point") point: MonitoringPoint
	) =
		UpdateMonitoringPointCommand(template, point)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint]) = {
		Mav("sysadmin/pointsettemplates/point/update_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("sysadmin/pointsettemplates/_monitoringPointsPersisted").noLayoutIf(ajax)
		}
	}

}

@Controller
@RequestMapping(Array("/sysadmin/pointsettemplates/{template}/edit/points/{point}/delete"))
class RemoveMonitoringPointController extends MonitoringPointSetTemplateController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable(value = "template") template: MonitoringPointSetTemplate,
		@PathVariable(value = "point") point: MonitoringPoint
	) =
		RemoveMonitoringPointCommand(template, point)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint]) = {
		Mav("sysadmin/pointsettemplates/point/remove_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("sysadmin/pointsettemplates/_monitoringPointsPersisted").noLayoutIf(ajax)
		}
	}

}
