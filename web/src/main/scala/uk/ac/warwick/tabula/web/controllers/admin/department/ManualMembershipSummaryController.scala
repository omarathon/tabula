package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, CurrentSITSAcademicYear, SelfValidating}
import uk.ac.warwick.tabula.commands.admin.department.ManualMembershipSummaryCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent, ManualMembershipInfo}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.AdminController

import scala.collection.immutable.TreeMap

@Controller
@RequestMapping(Array("/admin/department/{department}/manualmembership"))
class ManualMembershipSummaryController extends AdminController with DepartmentScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	type ManualMembershipSummaryCommand = Appliable[ManualMembershipInfo] with CurrentSITSAcademicYear

	validatesSelf[SelfValidating]

	@ModelAttribute("summaryCommand")
	def summaryCommand(@PathVariable department: Department): ManualMembershipSummaryCommand = ManualMembershipSummaryCommand(mandatory(department))

	override val departmentPermission: Permission = Permissions.Department.ViewManualMembershipSummary

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@Valid @ModelAttribute("summaryCommand") cmd: ManualMembershipSummaryCommand, @PathVariable department: Department): Mav = {

		val info = cmd.apply()
		val assignmentsByModule = info.assignments.groupBy(_.module)
		val smallGroupSetsByModule = info.smallGroupSets.groupBy(_.module)

		Mav("admin/manual-membership",
			"academicYear" -> cmd.getAcademicYearString,
			"returnTo" -> getReturnTo(Routes.admin.department(department)),
			"assignmentsByModule" -> TreeMap(assignmentsByModule.toSeq:_*),
			"smallGroupSetsByModule" -> TreeMap(smallGroupSetsByModule.toSeq:_*)
		).crumbs(Breadcrumbs.Department(department))
	}

	@RequestMapping(path = Array("eo"), method = Array(GET, HEAD))
	def eoForm(@Valid @ModelAttribute("summaryCommand") cmd: ManualMembershipSummaryCommand, @PathVariable department: Department): Mav = {
		val info = cmd.apply()
		val assignmentsByModule = info.assignments.groupBy(_.module)
		val smallGroupSetsByModule = info.smallGroupSets.groupBy(_.module)

		val modules = (assignmentsByModule.keys ++ smallGroupSetsByModule.keys).toSeq.distinct
		val studentsByModule = modules.map(module => module -> {
			val assignments = assignmentsByModule.getOrElse(module, Nil)
			val smallGroupSets = smallGroupSetsByModule.getOrElse(module, Nil)
			(assignments.flatMap(_.members.users) ++ smallGroupSets.flatMap(_.members.users)).distinct.sortBy(_.getWarwickId)
		}).toMap

		Mav("admin/manual-membership-eo",
			"academicYear" -> cmd.getAcademicYearString,
			"returnTo" -> getReturnTo(Routes.admin.home),
			"studentsByModule" -> TreeMap(studentsByModule.toSeq:_*)
		).crumbs(Breadcrumbs.Department(department))
	}

}
