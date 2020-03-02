package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, AutowiringSecurityServiceComponent, ModuleAndDepartmentServiceComponent}

object UniversityIdSearchCommand {
  def apply(academicYear: AcademicYear, user: CurrentUser) =
    new UniversityIdSearchCommandInternal(academicYear, user)
      with ComposableCommand[Seq[String]]
      with AutowiringProfileServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringSecurityServiceComponent
      with UserSearchCommandRequest
      with UserSearchCommandState
      with ReadOnly with Unaudited
}

abstract class UniversityIdSearchCommandInternal(val academicYear: AcademicYear, val user: CurrentUser) extends CommandInternal[Seq[String]] with FiltersStudents {

  self: UserSearchCommandRequest with UserSearchCommandState with ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): Seq[String] = {
    if (Option(department).isEmpty && serializeFilter.isEmpty) {
      throw new IllegalArgumentException("At least one filter value must be defined")
    }

    val restrictions = if (studentsOnly) {
      buildRestrictions(user, Seq(department), academicYear, Seq(groupNameRestriction ++ enrolmentDepartmentRestriction ++ studyRouteRestriction).flatten)
    } else {
      groupNameRestriction ++ homeDepartmentRestriction
    }.toSeq

    profileService.findAllUniversityIdsByRestrictions(
      restrictions,
      buildOrders()
    ).distinct
  }
}