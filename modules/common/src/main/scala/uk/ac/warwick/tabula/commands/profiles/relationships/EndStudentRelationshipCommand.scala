package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.{StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}
import uk.ac.warwick.tabula.permissions.Permissions

/**
 * End a student relationship
 */
class EndStudentRelationshipCommand(
	val relationship: StudentRelationship,
	val currentUser: CurrentUser
) extends AbstractEditStudentRelationshipCommand {

	var memberDao: MemberDao = Wire[MemberDao]

	override def oldAgents: Seq[Member] = relationship.agentMember.toSeq

	val relationshipType: StudentRelationshipType = relationship.relationshipType
	val studentCourseDetails: StudentCourseDetails = relationship.studentCourseDetails

	PermissionCheck(Permissions.Profiles.StudentRelationship.Manage(mandatory(relationshipType)), mandatory(studentCourseDetails))

	// throw this request out if the relationship can't be edited in Tabula for this department
	if (relationshipType.readOnly(mandatory(studentCourseDetails.department))) {
		logger.info("Denying access to EditStudentRelationshipCommand since relationship %s is read-only".format(relationshipType))
		throw new ItemNotFoundException()
	}

	def applyInternal(): Seq[StudentRelationship] = {
		relationship.endDate = DateTime.now
		relationshipService.saveOrUpdate(relationship)
		Seq(relationship)
	}

	override def describe(d: Description): Unit =
		d.studentIds(Seq(studentCourseDetails.student.universityId)).properties(
			"sprCode" -> studentCourseDetails.sprCode,
			"oldAgents" -> oldAgents.flatMap(_.universityId).mkString(" ")
		)

	def emit(relationships: Seq[StudentRelationship]): Seq[uk.ac.warwick.tabula.data.model.Notification[StudentRelationship,Unit]] = {
		val notifications = relationships.flatMap {
			relationship => {
				val studentNotification: Option[Notification[StudentRelationship, Unit]] =
					if (notifyStudent) {
						val notification = Notification.init(new StudentRelationshipChangeToStudentNotification, currentUser.apparentUser, Seq(relationship))
						notification.oldAgentIds.value = oldAgents.map(_.universityId)
						Some(notification)
					}
					else None

				val oldAgentNotification: Option[Notification[StudentRelationship, Unit]] =
					if (notifyOldAgents) {
						val notification = Notification.init(new StudentRelationshipChangeToOldAgentNotification, currentUser.apparentUser, Seq(relationship))
						notification.oldAgentIds.value = oldAgents.map(_.universityId)
						Some(notification)
					}
					else
						None

				(studentNotification ++ oldAgentNotification).toSeq
			}
		}
		notifications
	}

}