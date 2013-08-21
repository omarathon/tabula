package uk.ac.warwick.tabula.profiles.commands.tutor

import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.NoCurrentUser

class EditTutorCommandTest extends TestBase with Mockito {

	@Test
	def describeShouldIncludeNewTutorAndStudent { new TutorFixture {
		val command = new EditTutorCommand(studentCourseDetails, Some(oldTutor), NoCurrentUser(), false)
		command.tutor = newTutor
		val desc = mock[Description]
		// calls to desc.property are chained, so we need to set up the return
		desc.property("student SPR code", student.mostSignificantCourseDetails.get.sprCode) returns desc
		command.describe(desc)
		verify(desc, atLeastOnce()).property("student SPR code", student.mostSignificantCourseDetails.get.sprCode)
//		verify(desc, atLeastOnce()).property("student SPR code" -> studentCourseDetails.sprCode)
		verify(desc, atLeastOnce()).property("new tutor ID" -> newTutor.universityId)
	}}

	@Test
	def emitShouldCreateNotificationToTutee() { new TutorFixture{
		val command = new EditTutorCommand(studentCourseDetails, Some(oldTutor), NoCurrentUser(), false)
		command.tutor = newTutor
		command.notifyTutee = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student")) should be (true)
	}}

	@Test
	def emitShouldCreateNotificationToOldTutor() { new TutorFixture{
		val command = new EditTutorCommand(studentCourseDetails, Some(oldTutor), NoCurrentUser(), false)
		command.tutor = newTutor
		command.notifyOldTutor = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "0000002")) should be (true)
	}}

	@Test
	def emitShouldCreateNotificationToNewTutor() { new TutorFixture{
		val command = new EditTutorCommand(studentCourseDetails, Some(oldTutor), NoCurrentUser(), false)
		command.tutor = newTutor
		command.notifyNewTutor = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(1)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "0000001")) should be (true)
	}}

	@Test
	def emitShouldNotNotifyOldTutorIfTheyDontExist() { new TutorFixture{
		val command = new EditTutorCommand(studentCourseDetails, None, NoCurrentUser(), false)
		command.tutor = newTutor
		command.notifyNewTutor = true
		command.notifyOldTutor = true
		command.notifyTutee = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "0000001")) should be (true)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student")) should be (true)
	}}

	@Test
	def emitShouldNotifyOnRemove() { new TutorFixture{
		val command = new EditTutorCommand(studentCourseDetails, Some(oldTutor), NoCurrentUser(), false)
		command.tutor = newTutor
		command.notifyOldTutor = true
		command.notifyTutee = true

		val notifications = command.emit(Seq(relationship))
		notifications.size should be(2)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "0000002")) should be (true)
		notifications.exists(n=>n.recipients.exists(u=>u.getWarwickId == "student")) should be (true)
	}}

}
