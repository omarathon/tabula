package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.commands.{DescriptionImpl}
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.NoCurrentUser
import uk.ac.warwick.tabula.data.model.{MemberStudentRelationship, StaffMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.data.model.notifications.{StudentRelationshipChangeToStudentNotification, StudentRelationshipChangeToOldAgentNotification}

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
		// apply should return all the modified relationships

		val relationshipService = smartMock[RelationshipService]
		relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, List(newTutor)) returns Seq(StudentRelationship(newTutor, relationshipType, studentCourseDetails))
		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails) returns Seq(relationshipOld)

		// SCENARIO - no existing relationships for the SCD - add a new tutor and get back the new relationship:
		var command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(), NoCurrentUser(), false)
		command.agent = newTutor

		command.relationshipService = relationshipService

		var rels = command.applyInternal
		rels.size should be (1)
		rels.head.agent should be (newTutor.universityId)
		rels.head.studentMember should be (Some(studentCourseDetails.student))


		// SCENARIO - an existing relationship with a different tutor:
		command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.agent = newTutor

		command.relationshipService = relationshipService

		rels = command.applyInternal
		rels.size should be (1)
		rels.head.agent should be (newTutor.universityId)
		rels.head.studentMember should be (Some(studentCourseDetails.student))

		// SCENARIO - replace an agent with themselves, ie do nothing
		command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.agent = oldTutor
		command.relationshipService = relationshipService

		rels = command.applyInternal
		rels.size should be (0)

		// SCENARIO - multiple old tutors, just replace one
		val oldTutor2 = new StaffMember
		oldTutor2.universityId = "00000022"

		profileService.getMemberByUniversityId("00000022") returns Some(oldTutor2)

		val relationshipOld2 = new MemberStudentRelationship
		relationshipOld2.studentMember = student
		relationshipOld2.agentMember = oldTutor2

		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails) returns Seq(relationshipOld, relationshipOld2)

		command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor), NoCurrentUser(), false)
		command.relationshipService = relationshipService
		command.agent = newTutor

 		val rels3 = command.applyInternal

		// so we should have replaced oldTutor with newTutor and oldTutor2 should remain untouched
		rels3.size should be (1)
		rels3.head.agent should be (newTutor.universityId)
		rels3.head.studentMember should be (Some(studentCourseDetails.student))
		command.oldAgents should be (Seq(oldTutor))

		// SCENARIO - multiple old tutors, replace them all

		command = new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Seq(oldTutor, oldTutor2), NoCurrentUser(), false)
		command.relationshipService = relationshipService
		command.agent = newTutor

		val rels4 = command.applyInternal

		// so we should have replaced oldTutor and oldTutor2 with newTutor
		rels4.size should be (1)
		rels4.head.agent should be (newTutor.universityId)
		rels4.head.studentMember should be (Some(studentCourseDetails.student))
		command.oldAgents should be (Seq(oldTutor, oldTutor2))

	}}

}
