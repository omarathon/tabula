package uk.ac.warwick.tabula.api.commands.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order.asc
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JInteger, JList}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent}

object UniversityIdSearchCommand {
	def apply() =
		new UniversityIdSearchCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringProfileServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with UniversityIdSearchPermissions
			with UniversityIdSearchCommandRequest
			with ReadOnly with Unaudited
}


abstract class UniversityIdSearchCommandInternal extends CommandInternal[Seq[String]] with FiltersStudents {

	self: UniversityIdSearchCommandRequest with ModuleAndDepartmentServiceComponent =>

	override def applyInternal(): Seq[String] = {
		if (Option(department).isEmpty && serializeFilter.isEmpty) {
			throw new IllegalArgumentException("At least one filter value must be defined")
		}

		val restrictions = buildRestrictions(AcademicYear.now())
		val departments = Option(department) match {
			case Some(d) => Seq(d)
				case _ => moduleAndDepartmentService.allDepartments
		}
		departments.flatMap(dept =>
			profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
				dept,
				restrictions,
				buildOrders()
			)
		).distinct
	}

}

trait UniversityIdSearchPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.ViewSearchResults, PermissionsTarget.Global)
	}

}

trait UniversityIdSearchCommandRequest {
	var department: Department = _

	val defaultOrder = Seq(asc("lastName"), asc("firstName"))
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var courses: JList[Course] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var levelCodes: JList[String] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}
