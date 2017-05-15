package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.departments.ExtensionSettingsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/settings/extensions"))
class ExtensionSettingsController extends CourseworkController
	with DepartmentScopedController with AutowiringModuleAndDepartmentServiceComponent with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = ExtensionSettingsCommand.AdminPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("extensionSettingsCommand")
	def extensionSettingsCommand(@PathVariable department: Department): ExtensionSettingsCommand.Command =
		ExtensionSettingsCommand(mandatory(department))

	validatesSelf[SelfValidating]

	@RequestMapping
	def viewSettings = Mav("cm2/admin/extension-settings")

	@RequestMapping(method = Array(RequestMethod.POST))
	def saveSettings(@Valid @ModelAttribute("extensionSettingsCommand") cmd: ExtensionSettingsCommand.Command, errors: Errors): Mav =
		if (errors.hasErrors) {
			viewSettings
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(cmd.department))
		}

}