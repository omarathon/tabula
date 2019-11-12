package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{Aliasable, HibernateHelpers, ScalaRestriction}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent}

object UniversityIdSearchCommand {
  def apply(academicYear: AcademicYear) =
    new UniversityIdSearchCommandInternal(academicYear)
      with ComposableCommand[Seq[String]]
      with AutowiringProfileServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with UserSearchCommandRequest
      with UserSearchCommandState
      with ReadOnly with Unaudited
}

abstract class UniversityIdSearchCommandInternal(val academicYear: AcademicYear) extends CommandInternal[Seq[String]] with FiltersStudents {

  self: UserSearchCommandRequest with UserSearchCommandState with ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): Seq[String] = {
    if (Option(department).isEmpty && serializeFilter.isEmpty) {
      throw new IllegalArgumentException("At least one filter value must be defined")
    }

    val restrictions = if (studentsOnly) {
      buildRestrictions(academicYear) ++ groupNameRestriction ++ enrolmentDepartmentRestriction
    } else {
      groupNameRestriction ++ homeDepartmentRestriction
    }.toSeq

    profileService.findAllUniversityIdsByRestrictions(
      restrictions,
      buildOrders()
    ).distinct
  }
}