package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{MitigatingCircumstancesSubmitter, Role}

@Component
class MitigatingCircumstancesSubmitterRoleProvider extends RoleProvider with TaskBenchmarking {

  def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for MitigatingCircumstancesSubmitterRoleProvider") {
    scope match {
      case student: StudentMember if student.homeDepartment.subDepartmentsContaining(student).exists(_.enableMitCircs) => Stream(MitigatingCircumstancesSubmitter(student))
      // MitigatingCircumstancesSubmitter is only checked against students
      case _ => Stream.empty
    }
  }

  def rolesProvided: Set[Class[_ <: Role]] = Set(classOf[MitigatingCircumstancesSubmitter])

}