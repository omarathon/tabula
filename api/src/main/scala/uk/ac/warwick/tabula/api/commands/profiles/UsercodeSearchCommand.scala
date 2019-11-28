package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent}


object UserCodeSearchCommand {
  def apply(academicYear: AcademicYear) =
    new UserCodeSearchCommandInternal(academicYear)
      with ComposableCommand[Seq[String]]
      with AutowiringProfileServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with UserSearchCommandRequest
      with UserSearchCommandState
      with ReadOnly with Unaudited
}

abstract class UserCodeSearchCommandInternal(val academicYear: AcademicYear) extends CommandInternal[Seq[String]] with FiltersStudents {

  self: UserSearchCommandRequest with UserSearchCommandState with ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): Seq[String] = {
    if (Option(department).isEmpty && serializeFilter.isEmpty) {
      throw new IllegalArgumentException("At least one filter value must be defined")
    }

    val restrictions = if (studentsOnly) {
      buildRestrictions(academicYear, Seq(groupNameRestriction ++ enrolmentDepartmentRestriction ++ studyRouteRestriction).flatten)
    } else {
      groupNameRestriction ++ homeDepartmentRestriction
    }.toSeq

    profileService.findAllUserIdsByRestrictions(
      restrictions
    ).distinct
  }
}