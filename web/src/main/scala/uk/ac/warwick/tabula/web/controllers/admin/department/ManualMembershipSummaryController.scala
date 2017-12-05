package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, CurrentAcademicYear, SelfValidating}
import uk.ac.warwick.tabula.commands.admin.department.ManualMembershipSummaryCommand
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent, ManualMembershipInfo}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.TreeMap
import scala.collection.mutable

@Controller
@RequestMapping(Array("/admin/department/{department}/manualmembership"))
class ManualMembershipSummaryController extends AdminController with DepartmentScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	type ManualMembershipSummaryCommand = Appliable[ManualMembershipInfo] with CurrentAcademicYear

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

		import uk.ac.warwick.tabula.helpers.UserOrderingByIds._

		val info = cmd.apply()

		val studentModuleMap = mutable.Map[User, Set[Module]]()

		for(assignment <- info.assignments; student <- assignment.members.users) {
			studentModuleMap(student) = studentModuleMap.getOrElse(student, Set()) + assignment.module
		}

		for(smallGroupSet <- info.smallGroupSets; student <- smallGroupSet.members.users) {
			studentModuleMap(student) = studentModuleMap.getOrElse(student, Set()) + smallGroupSet.module
		}

		Mav("admin/manual-membership-eo",
			"academicYear" -> cmd.getAcademicYearString,
			"returnTo" -> getReturnTo(Routes.admin.home),
			"studentModuleMap" -> TreeMap(studentModuleMap.toSeq:_*)
		).crumbs(Breadcrumbs.Department(department))
	}

}
