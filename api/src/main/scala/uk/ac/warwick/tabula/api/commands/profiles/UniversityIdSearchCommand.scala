package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{Aliasable, HibernateHelpers, ScalaRestriction}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent}

object UniversityIdSearchCommand {
  def apply() =
    new UniversityIdSearchCommandInternal
      with ComposableCommand[Seq[String]]
      with AutowiringProfileServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with UserSearchCommandRequest
      with ReadOnly with Unaudited
}

abstract class UniversityIdSearchCommandInternal extends CommandInternal[Seq[String]] with FiltersStudents {

  self: UserSearchCommandRequest with ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): Seq[String] = {
    if (Option(department).isEmpty && serializeFilter.isEmpty) {
      throw new IllegalArgumentException("At least one filter value must be defined")
    }

    val restrictions = Option(department) match {
      case Some(_) => Seq(departmentRestriction) ++ buildRestrictions(AcademicYear.now())
      case _ => buildRestrictions(AcademicYear.now())
    }

    profileService.findAllUniversityIdsByRestrictions(
      restrictions,
      buildOrders()
    ).distinct
  }
}