package uk.ac.warwick.tabula.web.controllers.admin.department

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
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

	type ManualMembershipSummaryCommand = Appliable[ManualMembershipInfo]

	@ModelAttribute("command")
	def command(@PathVariable department: Department): ManualMembershipSummaryCommand = ManualMembershipSummaryCommand(mandatory(department))

	override val departmentPermission: Permission = Permissions.Department.ViewManualMembershipSummary

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable department: Department, @ModelAttribute("command") cmd: ManualMembershipSummaryCommand): Mav = {

		val info = cmd.apply()
		val assignmentsByModule = info.assignments.groupBy(_.module)
		val smallGroupSetsByModule = info.smallGroupSets.groupBy(_.module)


		Mav("admin/manual-membership",
			"returnTo" -> getReturnTo(Routes.admin.department(department)),
			"assignmentsByModule" -> TreeMap(assignmentsByModule.toSeq:_*),
			"smallGroupSetsByModule" -> TreeMap(smallGroupSetsByModule.toSeq:_*)
		).crumbs(Breadcrumbs.Department(department))
	}

}
