package uk.ac.warwick.tabula.commands.profiles.relationships

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.{Description, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Member, Notification, StudentCourseDetails, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.data.model.notifications.profiles.{StudentRelationshipChangeToNewAgentNotification, StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}
import uk.ac.warwick.tabula.helpers.MutablePromise
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.permissions.Permissions

/**
 * Command to edit the relationship for the StudentCourseDetails passed in, passing
 * in the current agent to change. This is passed in to distinguish
 * which agent is being "changed" if a student has multiple agents, or that an
 * agent is being added and the existing one shouldn't be removed.
 *
 */
class EditStudentRelationshipCommand(
	val studentCourseDetails: StudentCourseDetails,
	val relationshipType: StudentRelationshipType,
	val currentAgents: Seq[Member], // from the profile screen, this is the agent being changed - otherwise existing agents
	val currentUser: CurrentUser,
	val remove: Boolean // this is a remove action from the student profile screen

) extends AbstractEditStudentRelationshipCommand with SelfValidating {

	override def oldAgents: Seq[Member] = if (currentAgents == null) Seq[Member]() else currentAgents

	var agent: Member = _

	PermissionCheck(Permissions.Profiles.StudentRelationship.Manage(mandatory(relationshipType)), mandatory(studentCourseDetails))

	// throw this request out if the relationship can't be edited in Tabula for this department
	if (relationshipType.readOnly(mandatory(studentCourseDetails.department))) {
		logger.info("Denying access to EditStudentRelationshipCommand since relationship %s is read-only".format(relationshipType))
		throw new ItemNotFoundException()
	}

	val newAgent: MutablePromise[Member] = promise { agent }

	def validate(errors: Errors) {
		if(agent == null){
			errors.rejectValue("agent", "profiles.relationship.add.noAgent")
		}
	}

	/** applyInternal actions 3 different things:
		*
		* 1. From the student profile page, a user removes an agent (remove == true)
		* 2. From the student profile page, a user replaces a single specified agent (held in currentAgents) with another specified agent (agent)
		* 3. Using drag-and-drop/spreadsheet upload, a user replaces all existing agents (held in currentAgents) with another specified agent (agent)
		*
		* In case 3 currentAgents will be all the old agents but not necessarily in case 2.
		*
		* It returns a Seq of relationships it has modified, excluding relationships that have been ended as part of a change
		* allowing it to return enough information for notifications, given that the calling method also has access to oldAgents.
		*
		* If all this does is add an agent, the new relationship is returned.
		*
		* If an agent is changed, it returns the new relationship only -
		* notifications will use oldAgent to figure out what relationships have been ended.
		*
		* If the sole action is to remove an agent, the ended relationship is returned.
		*/

	def applyInternal(): Seq[StudentRelationship] = {

		if (currentAgents.isEmpty) {
			// student has no existing agent - just create a new relationship
			val newRelationship = relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, Seq(agent)).head
			Seq(newRelationship)
		}
		else if (!currentAgents.contains(agent)) {
				// we've been given a new agent -
				// replace the current agents with the new one and return the new relationship
				val relationshipsToReplace = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails).filter(rel => currentAgents.contains(rel.agentMember.getOrElse(throw new ItemNotFoundException)))
				relationshipService.endStudentRelationships(relationshipsToReplace)
				relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, Seq(agent))
		}
		else {
			if (remove) {
			// remove the relationship for the specified agent and return the ended relationship
			val preexistingRelationships = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)

			val relationshipsToRemove: Seq[StudentRelationship] = preexistingRelationships.filter(rel => rel.agentMember == Some(agent))
			relationshipService.endStudentRelationships(relationshipsToRemove)
			relationshipsToRemove
			}
			else {
				// the agent is already tutor for this student and this isn't a remove action - do nothing
				Nil
			}
		}
	}

	override def describe(d: Description): Unit = {
		val oldAgentsHere: Seq[Member] = oldAgents
		val oldAgentString =
			if (oldAgentsHere == null || oldAgentsHere.isEmpty ) ""
			else {
				val oldAgentUniIds = oldAgentsHere.map(_.universityId)
				oldAgentUniIds.mkString(" ")
			}
		d.studentIds(Seq(studentCourseDetails.student.universityId)).properties(
			"sprCode" -> studentCourseDetails.sprCode,
			"oldAgents" -> oldAgentString,
			"newAgent" -> Option(agent).fold("") {_.universityId}
		)
	}

	def emit(modifiedRelationships: Seq[StudentRelationship]): Seq[Notification[StudentRelationship, Unit]] = {
		val notifications = modifiedRelationships.flatMap(relationship => {

			val studentNotification: Option[Notification[StudentRelationship, Unit]] = if (notifyStudent) {
				val notification = Notification.init(new StudentRelationshipChangeToStudentNotification, currentUser.apparentUser, Seq(relationship))
				notification.oldAgentIds.value = currentAgents.map(_.universityId)
				Some(notification)
			} else None

			val oldAgentNotifications = if (notifyOldAgents) {
				currentAgents.map(oldAgent => {
					val notification = Notification.init(new StudentRelationshipChangeToOldAgentNotification, currentUser.apparentUser, Seq(relationship))
					notification.oldAgentIds.value = currentAgents.map(_.universityId)
					notification
				})
			} else Nil

			val newAgentNotification = if (notifyNewAgent) {
				relationship.agentMember.map(newAgent => {
					val notification = Notification.init(new StudentRelationshipChangeToNewAgentNotification, currentUser.apparentUser, Seq(relationship))
					notification.oldAgentIds.value = currentAgents.map(_.universityId)
					notification
				})
			} else None

			studentNotification ++ oldAgentNotifications ++ newAgentNotification
		})

		notifications
	}
}