package uk.ac.warwick.tabula.profiles.commands.relationships

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.notifications.{StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao

/**
 * End a student relationship
 */
class EndStudentRelationshipCommand(
	val relationship: StudentRelationship,
	val currentUser: CurrentUser
) extends AbstractEditStudentRelationshipCommand {

	var memberDao = Wire[MemberDao]

	def oldAgent = relationship.agentMember

	val relationshipType = relationship.relationshipType
	val studentCourseDetails = relationship.studentCourseDetails

	PermissionCheck(Permissions.Profiles.StudentRelationship.Update(mandatory(relationshipType)), mandatory(studentCourseDetails))

	// throw this request out if the relationship can't be edited in Tabula for this department
	if (relationshipType.readOnly(mandatory(studentCourseDetails.department))) {
		logger.info("Denying access to EditStudentRelationshipCommand since relationship %s is read-only".format(relationshipType))
		throw new ItemNotFoundException()
	}

	def applyInternal() = {
		relationship.endDate = DateTime.now
		relationshipService.saveOrUpdate(relationship)
		Seq(relationship)
	}

	override def describe(d: Description) = 
		d.property("student SPR code" -> studentCourseDetails.sprCode).property("old agent ID" -> oldAgent.map { _.universityId }.getOrElse(""))

	def emit(relationships: Seq[StudentRelationship]): Seq[uk.ac.warwick.tabula.data.model.Notification[StudentRelationship,Unit]] = {
		val notifications = relationships.flatMap {
			relationship => {
				val studentNotification: Option[Notification[StudentRelationship, Unit]] =
					if (notifyStudent)
						Some(Notification.init(new StudentRelationshipChangeToStudentNotification, currentUser.apparentUser, Seq(relationship)))
					else None

				val oldAgentNotification: Option[Notification[StudentRelationship, Unit]] =
					if (notifyOldAgent)
						Some(Notification.init(new StudentRelationshipChangeToOldAgentNotification, currentUser.apparentUser, Seq(relationship)))
					else
						None

				(studentNotification ++ oldAgentNotification).toSeq
			}
		}
		notifications
	}

}