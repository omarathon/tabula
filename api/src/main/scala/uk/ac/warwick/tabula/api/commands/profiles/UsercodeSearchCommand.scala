package uk.ac.warwick.tabula.api.commands.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order.asc
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JInteger, JList}
import uk.ac.warwick.tabula.commands.{CommandInternal, FiltersStudents}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object UsercodeSearchCommand {

}

class UsercodeSearchCommand {

}

abstract class UniversityIdSearchCommandInternal extends CommandInternal[Seq[String]] with FiltersStudents {

	self: UsercodeSearchCommandRequest with ModuleAndDepartmentServiceComponent =>

	override def applyInternal(): Seq[String] = {
		if (Option(department).isEmpty && serializeFilter.isEmpty) {
			throw new IllegalArgumentException("At least one filter value must be defined")
		}

		val restrictions = buildRestrictions(AcademicYear.now())

		Option(department) match {
			case Some(d) =>
				Seq(d).flatMap(dept =>

					profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
						dept,
						restrictions,
						buildOrders()
					)
				).distinct
			case _ =>
				profileService.findAllUniversityIdsByRestrictions(
					restrictions,
					buildOrders()
				).distinct
		}

		???
	}
}

trait UsercodeSearchCommandPermissions extends UniversityIdSearchPermissions

trait UsercodeSearchCommandRequest extends UniversityIdSearchCommandRequest