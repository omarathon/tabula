package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.ScheduledMeetingRecord
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class ScheduledMeetingRecordConfirmerRoleProvider extends RoleProvider with TaskBenchmarking {

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for ScheduledMeetingRecordConfirmerRoleProvider") {
		scope match {
			case meeting: ScheduledMeetingRecord if meeting.creator.universityId == user.universityId =>
				Stream(
					customRoleFor(meeting.creator.homeDepartment)(ScheduledMeetingRecordConfirmerRoleDefinition, meeting)
						.getOrElse(ScheduledMeetingRecordConfirmer(meeting))
				)

			// ScheduledMeetingRecordConfirmer is only checked at the meeting level
			case _ => Stream.empty
		}
	}

	def rolesProvided = Set(classOf[ScheduledMeetingRecordConfirmer])

}