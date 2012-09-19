package uk.ac.warwick.courses.web.controllers.sysadmin

import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid
import uk.ac.warwick.courses.commands.departments.AddDeptOwnerCommand
import uk.ac.warwick.courses.commands.departments.RemoveDeptOwnerCommand
import uk.ac.warwick.courses.commands.imports.ImportModulesCommand
import uk.ac.warwick.courses.commands.SelfValidating
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.AuditEventIndexService
import uk.ac.warwick.courses.services.MaintenanceModeService
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.DateFormats
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.services.AssignmentImporter
import uk.ac.warwick.courses.commands.imports.ImportAssignmentsCommand

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 */

abstract class BaseSysadminController extends BaseController {
	@Autowired var moduleService: ModuleAndDepartmentService = null
	@Autowired var userLookup: UserLookupInterface = _

	def redirectToHome = Redirect("/sysadmin/")

	def redirectToDeptOwners(deptcode: String) = Mav("redirect:/sysadmin/departments/" + deptcode + "/owners/")

	def viewDepartmentOwners(@PathVariable dept: Department): Mav =
		Mav("sysadmin/departments/owners",
			"department" -> dept,
			"owners" -> dept.owners)

	@ModelAttribute("reindexForm") def reindexForm = new ReindexForm

}

@Controller
@RequestMapping(Array("/sysadmin"))
class SysadminController extends BaseSysadminController {

	@Autowired var maintenanceService: MaintenanceModeService = _

	@RequestMapping
	def home = Mav("sysadmin/home").addObjects("maintenanceModeService" -> maintenanceService)

	@RequestMapping(value = Array("/departments/{dept}/owners/"), method = Array(GET))
	def departmentOwners(@PathVariable dept: Department) = viewDepartmentOwners(dept)

	@RequestMapping(Array("/departments/"))
	def departments = Mav("sysadmin/departments/list",
		"departments" -> moduleService.allDepartments)

	@RequestMapping(Array("/departments/{dept}/"))
	def department(@PathVariable dept: Department) = {
		Mav("sysadmin/departments/single",
			"department" -> dept)
	}

	@RequestMapping(value = Array("/import"), method = Array(POST))
	def importModules = {
		new ImportModulesCommand().apply()
		"sysadmin/importdone"
	}

}

@Controller
@RequestMapping(Array("/sysadmin/departments/{dept}/owners/delete"))
class RemoveDeptOwnerController extends BaseSysadminController {
	@ModelAttribute("removeOwner") def addOwnerForm(@PathVariable("dept") dept: Department) = {
		new RemoveDeptOwnerCommand(dept)
	}

	@RequestMapping(method = Array(POST))
	def addDeptOwner(@PathVariable dept: Department, @Valid @ModelAttribute("removeOwner") form: RemoveDeptOwnerCommand, errors: Errors) = {
		if (errors.hasErrors) {
			viewDepartmentOwners(dept)
		} else {
			logger.info("Passed validation, removing owner")
			form.apply()
			redirectToDeptOwners(dept.code)
		}
	}
}

@Controller
@RequestMapping(Array("/sysadmin/departments/{dept}/owners/add"))
class AddDeptOwnerController extends BaseSysadminController {

	validatesWith { (cmd: AddDeptOwnerCommand, errors: Errors) =>
		if (cmd.getUsercodes.contains(cmd.usercode)) {
			errors.rejectValue("usercode", "userId.duplicate")
		} else if (!userLookup.getUserByUserId(cmd.usercode).isFoundUser) {
			errors.rejectValue("usercode", "userId.notfound")
		}
	}

	@ModelAttribute("addOwner") def addOwnerForm(@PathVariable("dept") dept: Department) = {
		new AddDeptOwnerCommand(dept)
	}

	@RequestMapping(method = Array(GET))
	def showForm(@PathVariable dept: Department, @ModelAttribute("addOwner") form: AddDeptOwnerCommand, errors: Errors) = {
		Mav("sysadmin/departments/owners/add",
			"department" -> dept)
	}

	@RequestMapping(method = Array(POST))
	def submit(@PathVariable dept: Department, @Valid @ModelAttribute("addOwner") form: AddDeptOwnerCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(dept, form, errors)
		} else {
			logger.info("Passed validation, saving owner")
			form.apply()
			redirectToDeptOwners(dept.code)
		}
	}
}

@Configurable
class ReindexForm {
	@Autowired @BeanProperty var indexer: AuditEventIndexService = _

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var from: DateTime = _

	def reindex = {
		indexer.indexFrom(from)
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run"))
class SysadminIndexController extends BaseSysadminController {
	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexForm) = {
		form.reindex
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import-sits"))
class ImportSitsController extends BaseSysadminController {

	@Autowired var importer: AssignmentImporter = _

	@RequestMapping(method = Array(POST))
	def reindex() = {
		val command = new ImportAssignmentsCommand
		command.apply()
		redirectToHome
	}
}

class MaintenanceModeForm(service: MaintenanceModeService) extends SelfValidating {
	@BeanProperty var enable: Boolean = service.enabled

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var until: DateTime = service.until getOrElse DateTime.now.plusMinutes(30)

	@BeanProperty var message: String = service.message orNull

	def validate(implicit errors: Errors) {

	}
}

@Controller
@RequestMapping(Array("/sysadmin/maintenance"))
class MaintenanceModeController extends BaseSysadminController {
	@Autowired var service: MaintenanceModeService = _

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

			Redirect(Routes.sysadmin.home)
		}
	}

}
