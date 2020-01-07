package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{MeetingRecordApproverRoleDefinition, MeetingRecordApprover, Role}
import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class MeetingRecordApproverRoleProvider extends RoleProvider with TaskBenchmarking {

  def getRolesFor(user: CurrentUser, scope: PermissionsTarget): LazyList[Role] = benchmarkTask("Get roles for MeetingRecordApproverRoleProvider") {
    scope match {
      case meeting: MeetingRecord =>
        meeting.approvals.asScala.find(_.approver.universityId == user.universityId).map(approval => {
          LazyList(
            customRoleFor(approval.approver.homeDepartment)(MeetingRecordApproverRoleDefinition, approval)
              .getOrElse(MeetingRecordApprover(approval))
          )
        }).getOrElse(LazyList.empty)
      // MeetingRecordApprover is only checked at the meeting level
      case _ => LazyList.empty
    }
  }

  def rolesProvided = Set(classOf[MeetingRecordApprover])

}
