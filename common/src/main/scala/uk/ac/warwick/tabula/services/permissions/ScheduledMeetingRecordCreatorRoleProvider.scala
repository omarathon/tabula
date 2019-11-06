package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.ScheduledMeetingRecord
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class ScheduledMeetingRecordCreatorRoleProvider extends RoleProvider with TaskBenchmarking {

  def getRolesFor(user: CurrentUser, scope: PermissionsTarget): LazyList[Role] = benchmarkTask("Get roles for ScheduledMeetingRecordCreatorRoleProvider") {
    scope match {
      case meeting: ScheduledMeetingRecord if meeting.creator.universityId == user.universityId =>
        meeting.relationshipTypes.map { relationshipType =>
          customRoleFor(meeting.creator.homeDepartment)(ScheduledMeetingRecordCreatorRoleDefinition(relationshipType), meeting)
            .getOrElse(ScheduledMeetingRecordCreator(meeting, relationshipType))
        }.to(LazyList)

      // ScheduledMeetingRecordCreator is only checked at the meeting level
      case _ => LazyList.empty
    }
  }

  def rolesProvided = Set(classOf[ScheduledMeetingRecordCreator])

}
