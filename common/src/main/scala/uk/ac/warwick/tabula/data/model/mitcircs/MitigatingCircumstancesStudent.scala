package uk.ac.warwick.tabula.data.model.mitcircs

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions.PermissionsTarget

case class MitigatingCircumstancesStudent(student: StudentMember) extends PermissionsTarget {

  override def id: String = student.universityId

  override def permissionsParents: LazyList[PermissionsTarget] = {
    student.mostSignificantCourse.department.subDepartmentsContaining(student).filter(_.enableMitCircs)
  }

}
