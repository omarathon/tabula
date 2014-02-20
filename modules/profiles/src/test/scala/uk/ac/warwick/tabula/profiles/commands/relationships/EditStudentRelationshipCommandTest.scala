package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.NoCurrentUser
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.notifications.{StudentRelationshipChangeToStudentNotification, StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToNewAgentNotification}

class EditStudentRelationshipCommandTest extends TestBase with Mockito {
	
	val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

	@Test
	def describeShouldIncludeNewTutorAndStudent { new TutorFixture {
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Some(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		val desc = mock[Description]
		// calls to desc.property are chained, so we need to set up the return
		desc.property("student SPR code", student.mostSignificantCourseDetails.get.sprCode) returns desc
		command.describe(desc)
		verify(desc, atLeastOnce()).property("student SPR code", student.mostSignificantCourseDetails.get.sprCode)
//		verify(desc, atLeastOnce()).property("student SPR code" -> studentCourseDetails.sprCode)
		verify(desc, atLeastOnce()).property("new agent ID" -> newTutor.universityId)
	}}

	@Test
	def emitShouldCreateNotificationToTutee() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Some(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyStudent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student")) should be (true)
	}}

	@Test
	def emitShouldCreateNotificationToOldTutor() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Some(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyOldAgent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
	}}

	@Test
	def emitShouldCreateNotificationToNewTutor() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Some(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyNewAgent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
	}}

	@Test
	def emitShouldNotNotifyOldTutorIfTheyDontExist() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, None, NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyNewAgent = true
		command.notifyOldAgent = true
		command.notifyStudent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
	}}

	@Test
	def emitShouldNotifyOnRemove() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Some(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyOldAgent = true
		command.notifyStudent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
		notifications.exists(_.isInstanceOf[StudentRelationshipChangeToOldAgentNotification]) should be (true)
		notifications.exists(_.isInstanceOf[StudentRelationshipChangeToStudentNotification]) should be (true)
	}}

}
