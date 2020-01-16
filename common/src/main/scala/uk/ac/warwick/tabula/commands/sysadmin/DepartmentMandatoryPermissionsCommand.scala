package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.model.{CourseType, Department}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{MitigatingCircumstancesOfficerRoleDefinition, UserAccessMgrRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, ModuleAndDepartmentServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._

case class DepartmentMandatoryPermissionsInfo(
  department: Department,
  activeStudents: Map[CourseType, Int],
  hasActiveUAM: Boolean,
  hasActiveMCO: Boolean,
  children: Seq[DepartmentMandatoryPermissionsInfo] = Nil
)

object DepartmentMandatoryPermissionsCommand {
  type Result = Seq[DepartmentMandatoryPermissionsInfo]
  type Command = Appliable[Result]
  val RequiredPermission: Permission = Permissions.ReviewMitCircsDepartments

  def apply(): Command =
    new DepartmentMandatoryPermissionsCommandInternal
      with ComposableCommand[Result]
      with DepartmentMandatoryPermissionsCommandPermissions
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringPermissionsServiceComponent
      with Unaudited with ReadOnly
}

abstract class DepartmentMandatoryPermissionsCommandInternal extends CommandInternal[Seq[DepartmentMandatoryPermissionsInfo]] {
  self: ModuleAndDepartmentServiceComponent
    with ProfileServiceComponent
    with PermissionsServiceComponent =>

  private def calculateMandatoryPermissionInfo(department: Department): Option[DepartmentMandatoryPermissionsInfo] = {
    def countActiveStudents(courseTypes: CourseType*): Int =
      profileService.countStudentsByRestrictions(department, AcademicYear.now(),
        ScalaRestriction.startsWithIfNotEmpty(
          "course.code", courseTypes.flatMap(_.courseCodeChars.map(_.toString)),
          FiltersStudents.AliasPaths("course"): _*
        ).toSeq
      )

    val activeStudents: Map[CourseType, Int] =
      CourseType.all.map(courseType => courseType -> countActiveStudents(courseType)).toMap

    if (activeStudents.forall { case (_, count) => count == 0 }) None
    else {
      val info = DepartmentMandatoryPermissionsInfo(
        department,
        activeStudents,
        hasActiveUAM = permissionsService.getGrantedRole(department, UserAccessMgrRoleDefinition).toSeq.flatMap(_.users.users).exists { u => u.isFoundUser && !u.isLoginDisabled },
        hasActiveMCO = permissionsService.getGrantedRole(department, MitigatingCircumstancesOfficerRoleDefinition).toSeq.flatMap(_.users.users).exists { u => u.isFoundUser && !u.isLoginDisabled }
      )

      if (!department.hasChildren) {
        Some(info)
      } else {
        // For each child department, subtract values from the total
        val childInfo = department.children.asScala.toSeq.sortBy(_.code).flatMap(calculateMandatoryPermissionInfo)

        Some(info.copy(
          activeStudents = activeStudents.map { case (courseType, total) =>
            courseType -> (total - childInfo.map(_.activeStudents(courseType)).sum)
          },
          children = childInfo
        ))
      }
    }
  }

  override def applyInternal(): Seq[DepartmentMandatoryPermissionsInfo] = {
    val futures =
      moduleAndDepartmentService.allDepartments.filterNot(_.hasParent)
        .map { department =>
          Future {
            calculateMandatoryPermissionInfo(department)
          }
        }

    Await.result(Future.sequence(futures), Duration.Inf).flatten
  }
}

trait DepartmentMandatoryPermissionsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(DepartmentMandatoryPermissionsCommand.RequiredPermission, PermissionsTarget.Global)
}
