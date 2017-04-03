package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.admin.department.OldDisplaySettingsCommand
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.{CourseType, Department, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent, RelationshipService}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/admin/department/{department}/settings/display"))
class OldDisplaySettingsController extends AdminController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {
	
	var relationshipService: RelationshipService = Wire[RelationshipService]

	type OldDisplaySettingsCommand = Appliable[Department] with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("displaySettingsCommand")
	def oldDisplaySettingsCommand(@PathVariable department: Department): OldDisplaySettingsCommand =
		OldDisplaySettingsCommand(mandatory(department))

	@ModelAttribute("allRelationshipTypes") def allRelationshipTypes: Seq[StudentRelationshipType] = relationshipService.allStudentRelationshipTypes

	override val departmentPermission: Permission = Permissions.Department.ManageDisplaySettings

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping(method=Array(GET, HEAD))
	def initialView(@PathVariable department: Department, @ModelAttribute("displaySettingsCommand") cmd: OldDisplaySettingsCommand): Mav = {
		cmd.populate()
		viewSettings(department)
	}

	private def viewSettings(department: Department) =
		Mav("admin/display-settings",
			"department" -> department,
			"expectedCourseTypes" -> Seq(CourseType.UG, CourseType.PGT, CourseType.PGR, CourseType.Foundation, CourseType.PreSessional),
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Department(department)
		)

	@RequestMapping(method=Array(POST))
	def saveSettings(
		@Valid @ModelAttribute("displaySettingsCommand") cmd: OldDisplaySettingsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		if (errors.hasErrors){
			viewSettings(department)
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(department))
		}
	}
}