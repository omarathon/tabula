package uk.ac.warwick.tabula.profiles.commands.tutor

import org.joda.time.DateTime

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.profiles.notifications.TutorChangeNotification
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer

/**
 * Command to edit the tutor for the StudentCourseDetails passed in, passing 
 * in the current tutor if the student has one. This is passed in to distinguish 
 * which tutor is being "changed" if a student has multiple tutors, or that a 
 * tutor is being added and the existing one shouldn't be removed.
 * 
 * The command returns a Seq of the modified relationships - so if nothing has
 * changed, it will return Nil - but it may also return relationships that have
 * been ended with other tutors (for example when removing multiple tutors) that
 * weren't initially requested.
 */
class EditTutorCommand(val studentCourseDetails: StudentCourseDetails, val currentTutor: Option[Member], val currentUser: CurrentUser, val remove: Boolean)
	extends Command[Seq[StudentRelationship]] 
	with Notifies[Seq[StudentRelationship], StudentRelationship] {

	var relationshipService = Wire[RelationshipService]

	var tutor: Member = _

	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, studentCourseDetails.student)

	// throw this request out if personal tutors can't be edited in Tabula for this department
	if (!studentCourseDetails.department.canEditPersonalTutors) {
		logger.info("Denying access to EditTutorCommand since student "
				+ studentCourseDetails.sprCode
				+ " has a study department "
				+ "( " + studentCourseDetails.department.name
				+ ") with a personal tutor source setting of "
				+ studentCourseDetails.department.personalTutorSource + ".")
		throw new ItemNotFoundException()
	}

	val newTutor = promise { tutor }

	var notifyTutee: Boolean = false
	var notifyOldTutor: Boolean = false
	var notifyNewTutor: Boolean = false

	def applyInternal = {
		if (!currentTutor.isDefined) {
			// Brand new tutor
			val newRelationship = relationshipService.saveStudentRelationship(PersonalTutor, studentCourseDetails.sprCode, tutor.universityId)

			Seq(newRelationship)
		} else if (currentTutor.get != tutor) {
			// Replacing the current tutor with a new one
			val currentRelationships = relationshipService.findCurrentRelationships(PersonalTutor, studentCourseDetails.sprCode)

			// Is there an existing relationship for this tutor?
			// Could happen if a student has two tutors, and we're trying to replace the second with the first
			currentRelationships.find(_.agent == tutor.universityId) match {
				case Some(existingRelationship) => {
					// Just return the existing relationship without any notifications
					Nil
				}
				case _ => {
					// Find the relationship for the current tutor, and end it
					endTutorRelationship(currentRelationships)

					// Save the new relationship
					val newRelationship = relationshipService.saveStudentRelationship(PersonalTutor, studentCourseDetails.sprCode, tutor.universityId)

					Seq(newRelationship)
				}
			}
		} else if (currentTutor.get == tutor && remove) {		
			val currentRelationships = relationshipService.findCurrentRelationships(PersonalTutor, studentCourseDetails.sprCode)
			endTutorRelationship(currentRelationships)
						
			currentRelationships
		} else {
			Nil
		}
	}

	def endTutorRelationship(currentRelationships: Seq[StudentRelationship]) {
		currentRelationships.find(_.agent == currentTutor.get.universityId) foreach { rel =>
			rel.endDate = DateTime.now
			relationshipService.saveOrUpdate(rel)
		}
	}

	override def describe(d: Description) = d.property("student SPR code" -> studentCourseDetails.sprCode).property("new tutor ID" -> tutor.universityId)

	def emit(modifiedRelationships: Seq[StudentRelationship]): Seq[Notification[StudentRelationship]] = {	
		val notifications = modifiedRelationships.flatMap(relationship => {

			val tuteeNotification:List[Notification[StudentRelationship]] = if (notifyTutee) {
				val template = TutorChangeNotification.TuteeTemplate
				val recepient = relationship.studentMember.get.asSsoUser
				List(new TutorChangeNotification(relationship, currentUser.apparentUser, recepient, currentTutor, template) with FreemarkerTextRenderer)
			} else Nil

			val oldTutorNotification:List[Notification[StudentRelationship]] = if (notifyOldTutor) {			
				val notifications = currentTutor.map(oldTutor => {
					val template = TutorChangeNotification.OldTutorTemplate
					val recepient =  oldTutor.asSsoUser
					new TutorChangeNotification(relationship, currentUser.apparentUser, recepient, currentTutor, template) with FreemarkerTextRenderer
				})
				List(notifications).flatten
			} else Nil

			val newTutorNotification:List[Notification[StudentRelationship]] = if (notifyNewTutor) {
				val notifications = relationship.agentMember.map(newTutor => {
					val template = TutorChangeNotification.NewTutorTemplate
					val recepient = newTutor.asSsoUser
					new TutorChangeNotification(relationship, currentUser.apparentUser, recepient, currentTutor, template) with FreemarkerTextRenderer
				})
				List(notifications).flatten
			} else Nil

			tuteeNotification ++ oldTutorNotification ++ newTutorNotification
		})
		
		notifications
	}
}