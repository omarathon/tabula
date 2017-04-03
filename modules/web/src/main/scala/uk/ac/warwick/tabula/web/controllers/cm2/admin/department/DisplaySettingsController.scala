package uk.ac.warwick.tabula.web.controllers.cm2.admin.department

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.cm2.admin.department.DisplaySettingsCommand
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.{CourseType, Department, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/settings/display"))
class DisplaySettingsController extends CourseworkController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {
	
	var relationshipService: RelationshipService = Wire[RelationshipService]

	type DisplaySettingsCommand = Appliable[Department] with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("displaySettingsCommand")
	def displaySettingsCommand(@PathVariable department: Department): DisplaySettingsCommand =
		DisplaySettingsCommand(mandatory(department))

	@ModelAttribute("allRelationshipTypes") def allRelationshipTypes: Seq[StudentRelationshipType] = relationshipService.allStudentRelationshipTypes

	override val departmentPermission: Permission = Permissions.Department.ManageDisplaySettings

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	val academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

	@RequestMapping(method=Array(GET, HEAD))
	def initialView(@PathVariable department: Department, @ModelAttribute("displaySettingsCommand") cmd: DisplaySettingsCommand): Mav = {
		cmd.populate()
		viewSettings(department)
	}

	private def viewSettings(department: Department) =
		Mav(s"$urlPrefix/admin/display-settings",
			"department" -> department,
			"expectedCourseTypes" -> Seq(CourseType.UG, CourseType.PGT, CourseType.PGR, CourseType.Foundation, CourseType.PreSessional),
			"returnTo" -> getReturnTo("")
		)

	@RequestMapping(method=Array(POST))
	def saveSettings(
		@Valid @ModelAttribute("displaySettingsCommand") cmd: DisplaySettingsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		if (errors.hasErrors){
			viewSettings(department)
		} else {
			cmd.apply()
			Redirect(Routes.cm2.admin.department(department, academicYear))
		}
	}
}