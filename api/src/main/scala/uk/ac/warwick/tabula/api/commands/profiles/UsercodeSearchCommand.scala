package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent}


object UserCodeSearchCommand {
  def apply() =
    new UserCodeSearchCommandInternal
      with ComposableCommand[Seq[String]]
      with AutowiringProfileServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with UserSearchCommandRequest
      with ReadOnly with Unaudited
}

abstract class UserCodeSearchCommandInternal extends CommandInternal[Seq[String]] with FiltersStudents {

  self: UserSearchCommandRequest with ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): Seq[String] = {
    if (Option(department).isEmpty && serializeFilter.isEmpty) {
      throw new IllegalArgumentException("At least one filter value must be defined")
    }

    val restrictions = Option(department) match {
      case Some(_) => Seq(departmentRestriction) ++ buildRestrictions(AcademicYear.now())
      case _ => buildRestrictions(AcademicYear.now())
    }

    profileService.findAllUserIdsByRestrictions(
      restrictions
    ).distinct
  }
}