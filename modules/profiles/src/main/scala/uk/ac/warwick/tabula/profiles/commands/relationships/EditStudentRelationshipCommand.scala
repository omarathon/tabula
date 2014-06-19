package uk.ac.warwick.tabula.profiles.commands.relationships

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.{SelfValidating, Command, Description, Notifies}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.notifications.{StudentRelationshipChangeToNewAgentNotification, StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}

/**
 * Command to edit the relationship for the StudentCourseDetails passed in, passing 
 * in the current agent if the student has one. This is passed in to distinguish 
 * which agent is being "changed" if a student has multiple agents, or that an 
 * agent is being added and the existing one shouldn't be removed.
 * 
 * The command returns a Seq of the modified relationships - so if nothing has
 * changed, it will return Nil - but it may also return relationships that have
 * been ended with other agents (for example when removing multiple agents) that
 * weren't initially requested.
 */
class EditStudentRelationshipCommand(
	val studentCourseDetails: StudentCourseDetails,
	val relationshipType: StudentRelationshipType,
	val currentAgent: Option[Member],
	val currentUser: CurrentUser,
	val remove: Boolean
) extends AbstractEditStudentRelationshipCommand with SelfValidating {

	def oldAgent = currentAgent

	var agent: Member = _

	PermissionCheck(Permissions.Profiles.StudentRelationship.Update(mandatory(relationshipType)), mandatory(studentCourseDetails))

	// throw this request out if the relationship can't be edited in Tabula for this department
	if (relationshipType.readOnly(mandatory(studentCourseDetails.department))) {
		logger.info("Denying access to EditStudentRelationshipCommand since relationship %s is read-only".format(relationshipType))
		throw new ItemNotFoundException()
	}

	val newAgent = promise { agent }

	def validate(errors: Errors) {
		if(agent == null){
			errors.rejectValue("agent", "profiles.relationship.add.noAgent")
		}
	}

	def applyInternal() = {
		if (!currentAgent.isDefined) {
			// Brand new agent
			val newRelationship = relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, Seq(agent)).head

			Seq(newRelationship)
		} else if (currentAgent.get != agent) {
			// Replacing the current agent with a new one
			val currentRelationships = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)

			// Is there an existing relationship for this agent?
			// Could happen if a student has two agents, and we're trying to replace the second with the first
			currentRelationships.find(_.agent == agent.universityId) match {
				case Some(existingRelationship) =>
					// Just return the existing relationship without any notifications
					Nil
				case _ =>
					// Find the relationship for the current agent, and end it
					endAgentRelationship(currentRelationships)

					// Save the new relationship
					val newRelationship = relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, Seq(agent)).head

					Seq(newRelationship)
			}
		} else if (currentAgent.get == agent && remove) {		
			val currentRelationships = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)
			endAgentRelationship(currentRelationships)
						
			currentRelationships
		} else {
			Nil
		}
	}

	def endAgentRelationship(currentRelationships: Seq[StudentRelationship]) {
		currentRelationships.find(_.agent == currentAgent.get.universityId) foreach { rel =>
			rel.endDate = DateTime.now
			relationshipService.saveOrUpdate(rel)
		}
	}

	override def describe(d: Description) = 
		d.property("student SPR code" -> studentCourseDetails.sprCode)
		 .property("new agent ID" -> Option(agent).map { _.universityId }.getOrElse(""))

	def emit(modifiedRelationships: Seq[StudentRelationship]) = {
		val notifications = modifiedRelationships.flatMap(relationship => {

			val studentNotification: Option[Notification[StudentRelationship, Unit]] = if (notifyStudent) {
				Some(Notification.init(new StudentRelationshipChangeToStudentNotification, currentUser.apparentUser, Seq(relationship)))
			} else None

			val oldAgentNotification = if (notifyOldAgent) {
				currentAgent.map(oldAgent => {
					Notification.init(new StudentRelationshipChangeToOldAgentNotification, currentUser.apparentUser, Seq(relationship))
				})
			} else None

			val newAgentNotification = if (notifyNewAgent) {
				relationship.agentMember.map(newAgent => {
					Notification.init(new StudentRelationshipChangeToNewAgentNotification, currentUser.apparentUser, Seq(relationship))
				})
			} else None

			studentNotification ++ oldAgentNotification ++ newAgentNotification
		})
		
		notifications
	}
}