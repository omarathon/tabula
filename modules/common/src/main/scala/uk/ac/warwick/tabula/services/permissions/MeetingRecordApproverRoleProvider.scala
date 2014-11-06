package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{MeetingRecordApproverRoleDefinition, MeetingRecordApprover, Role}
import collection.JavaConverters._

@Component
class MeetingRecordApproverRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = {
		scope match {
			case meeting: MeetingRecord =>
				meeting.approvals.asScala.find(_.approver.universityId == user.universityId).map(approval => {
					Stream(
						customRoleFor(approval.approver.homeDepartment)(MeetingRecordApproverRoleDefinition, meeting)
							.getOrElse(MeetingRecordApprover(meeting))
					)
				}).getOrElse(Stream.empty)
			// MeetingRecordApprover is only checked at the meeting level
			case _ => Stream.empty
		}
	}
	
	def rolesProvided = Set(classOf[MeetingRecordApprover])
	
}