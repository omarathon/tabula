package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.{DescriptionImpl, Description}
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.NoCurrentUser
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.notifications.{StudentRelationshipChangeToStudentNotification, StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToNewAgentNotification}

class EditStudentRelationshipCommandTest extends TestBase with Mockito {
	
	val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

	@Test
	def describeShouldIncludeNewTutorAndStudent { new TutorFixture {
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		val desc = new DescriptionImpl
		command.describe(desc)

		desc.allProperties should be (Map(
			"students" -> List("student"),
			"sprCode" -> "0000001/1",
			"oldAgents" -> "0000002",
			"newAgent" -> "0000001"
		))
	}}

	@Test
	def emitShouldCreateNotificationToTutee() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyStudent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student")) should be (true)
	}}

	@Test
	def emitShouldCreateNotificationToOldTutor() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyOldAgents = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
	}}

	@Test
	def emitShouldCreateNotificationToNewTutor() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyNewAgent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
	}}

	@Test
	def emitShouldNotNotifyOldTutorIfTheyDontExist() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Nil, NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyNewAgent = true
		command.notifyOldAgents = true
		command.notifyStudent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
	}}

	@Test
	def emitShouldNotifyOnRemove() { new TutorFixture{
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor
		command.notifyOldAgents = true
		command.notifyStudent = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
		notifications.exists(_.isInstanceOf[StudentRelationshipChangeToOldAgentNotification]) should be (true)
		notifications.exists(_.isInstanceOf[StudentRelationshipChangeToStudentNotification]) should be (true)
	}}

	@Test
	def testApply() { new TutorFixture {
		val command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)

		val rels = command.applyInternal
	}}

}
