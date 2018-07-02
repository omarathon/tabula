package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent}


object UsercodeSearchCommand {
	def apply() =
		new UsercodeSearchCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringProfileServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with UsercodeSearchCommandPermissions
			with UsercodeSearchCommandRequest
			with ReadOnly with Unaudited
}

abstract class UsercodeSearchCommandInternal extends CommandInternal[Seq[String]] with FiltersStudents {

	self: UsercodeSearchCommandRequest with ModuleAndDepartmentServiceComponent =>

	override def applyInternal(): Seq[String] = {
		if (Option(department).isEmpty && serializeFilter.isEmpty) {
			throw new IllegalArgumentException("At least one filter value must be defined")
		}

		val restrictions = buildRestrictions(AcademicYear.now())

		Option(department) match {
			case Some(d) =>
				Seq(d).flatMap(department =>
					profileService.findAllUserIdsByRestrictionsInAffiliatedDepartments(
						department,
						restrictions
					)
				).distinct
			case _ =>
				profileService.findAllUserIdsByRestrictions(restrictions).distinct
		}
	}
}

trait UsercodeSearchCommandPermissions extends UniversityIdSearchPermissions

trait UsercodeSearchCommandRequest extends UniversityIdSearchCommandRequest