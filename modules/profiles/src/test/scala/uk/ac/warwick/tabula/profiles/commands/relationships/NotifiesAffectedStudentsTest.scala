package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.data.model.notifications.{BulkStudentRelationshipNotification, BulkNewAgentRelationshipNotification, BulkOldAgentRelationshipNotification, StudentRelationshipChangeNotification}
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentRelationship, StudentRelationshipType, Notification}
import uk.ac.warwick.tabula.services.{RelationshipService, ProfileService}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class NotifiesAffectedStudentsTest extends TestBase with Mockito {

	trait Environment {
		val Tutor = StudentRelationshipType("1", "tutor", "tutor", "tutee")

		val department = Fixtures.department("in", "IT Services")
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student1 = Fixtures.student("0000001", "student1", department)
		val student2 = Fixtures.student("0000002", "student2", department)
		val student3 = Fixtures.student("0000003", "student3", department)
		val student4 = Fixtures.student("0000004", "student4", department)
		val student5 = Fixtures.student("0000005", "student5", department)
		val student6 = Fixtures.student("0000006", "student6", department)
		val student7 = Fixtures.student("0000007", "student7", department)

		val staff1 = Fixtures.staff("1000001", "staff1", department)
		val staff2 = Fixtures.staff("1000002", "staff2", department)
		val staff3 = Fixtures.staff("1000003", "staff3", department)
		val staff4 = Fixtures.staff("1000004", "staff4", department)
		val staff5 = Fixtures.staff("1000005", "staff5", department)

		val rel1 = StudentRelationship(staff1, relationshipType, student1)
		val rel2 = StudentRelationship(staff1, relationshipType, student2)
		val rel3 = StudentRelationship(staff2, relationshipType, student3)

		var relationshipService = smartMock[RelationshipService]
		var profileService = smartMock[ProfileService]

		profileService.getMemberByUniversityId("1000001", false, false) returns Some(staff1)
		profileService.getMemberByUniversityId("1000002", false, false) returns Some(staff2)
		profileService.getMemberByUniversityId("1000004", false, false) returns Some(staff4)
		profileService.getMemberByUniversityId("1000005", false, false) returns Some(staff5)

	}

	@Test
	def testEmit: Unit = {
		new Environment {
			val cmd = new NotifiesAffectedStudents with RelationshipChangingCommand with Environment {
				val apparentUser: User = smartMock[User]
			}

			// one modified relationship and no old agents - it must be a new relationship, not replacing anything
			var oldAgents = Seq[StaffMember]()
			val modifiedRelationship = rel1
			var change = new StudentRelationshipChange(oldAgents, rel1)

			var relChanges = Seq[StudentRelationshipChange](change)

			cmd.notifyNewAgent = true
			cmd.notifyOldAgents = true
			cmd.notifyStudent = true

			var notifications: Seq[Notification[StudentRelationship, Unit]] = cmd.emit(relChanges)

			notifications.size should be (2)

			var newAgentNotifications = notifications.filter(_.isInstanceOf[BulkNewAgentRelationshipNotification])
			newAgentNotifications.size should be (1)

			var studentNotifications = notifications.filter(_.isInstanceOf[BulkStudentRelationshipNotification]).asInstanceOf[Seq[BulkStudentRelationshipNotification]]
			studentNotifications.size should be (1)
			studentNotifications.head.oldAgents.isEmpty should be (true)


			// one modified relationship and one old agent
			oldAgents = Seq(staff2)
			change = new StudentRelationshipChange(oldAgents, rel1)

			relChanges = Seq[StudentRelationshipChange](change)

			notifications = cmd.emit(relChanges)

			notifications.size should be (3)

			newAgentNotifications = notifications.filter(_.isInstanceOf[BulkNewAgentRelationshipNotification])
			newAgentNotifications.size should be (1)
			newAgentNotifications.head.recipients should be (List(staff1.asSsoUser))

			studentNotifications = notifications.filter(_.isInstanceOf[BulkStudentRelationshipNotification]).asInstanceOf[Seq[BulkStudentRelationshipNotification]]
			studentNotifications.size should be (1)
			studentNotifications.head.oldAgents.head should be (staff2)
			studentNotifications.head.recipients should be (List(student1.asSsoUser))

			var oldAgentNotifications: Seq[BulkOldAgentRelationshipNotification] = notifications.filter(_.isInstanceOf[BulkOldAgentRelationshipNotification]).asInstanceOf[Seq[BulkOldAgentRelationshipNotification]]
			oldAgentNotifications.size should be (1)
			var oldAgentNotification: BulkOldAgentRelationshipNotification = oldAgentNotifications.head
			oldAgentNotification.recipients should be (oldAgents.map(_.asSsoUser))


			// two modified relationships, each with one old agent
			var oldAgentsForRel1 = Seq(staff4)
			change = new StudentRelationshipChange(oldAgentsForRel1, rel1)

			var oldAgentsForRel3 = Seq(staff5)
			var change2 = new StudentRelationshipChange(oldAgentsForRel3, rel3)

			relChanges = Seq[StudentRelationshipChange](change, change2)

			notifications = cmd.emit(relChanges)

			notifications.size should be (6)

			newAgentNotifications = notifications.filter(_.isInstanceOf[BulkNewAgentRelationshipNotification])
			newAgentNotifications.size should be (2)
			newAgentNotifications.head.recipients should be (List(staff1.asSsoUser))

			studentNotifications = notifications.filter(_.isInstanceOf[BulkStudentRelationshipNotification]).asInstanceOf[Seq[BulkStudentRelationshipNotification]]
			studentNotifications.size should be (2)
			val studentNotificationForStudent1 = studentNotifications.filter(_.recipients.contains(student1.asSsoUser)).head
			studentNotificationForStudent1.oldAgents should be (Seq(staff4))

			oldAgentNotifications = notifications.filter(_.isInstanceOf[BulkOldAgentRelationshipNotification]).asInstanceOf[Seq[BulkOldAgentRelationshipNotification]]
			oldAgentNotifications.size should be (2)
			val oldAgentNotificationForStaff4: BulkOldAgentRelationshipNotification = oldAgentNotifications.filter(_.recipients.contains(staff4.asSsoUser)).head
			oldAgentNotificationForStaff4.oldAgents should be (Seq(staff4))


			// two modified relationships with same old agent
			oldAgentsForRel1 = Seq(staff4)
			change = new StudentRelationshipChange(oldAgentsForRel1, rel1)

			oldAgentsForRel3 = Seq(staff4)
			change2 = new StudentRelationshipChange(oldAgentsForRel3, rel3)

			relChanges = Seq[StudentRelationshipChange](change, change2)

			notifications = cmd.emit(relChanges)

			notifications.size should be (5)

			oldAgentNotifications = notifications.filter(_.isInstanceOf[BulkOldAgentRelationshipNotification]).asInstanceOf[Seq[BulkOldAgentRelationshipNotification]]
			oldAgentNotifications.size should be (1)
			oldAgentNotification = oldAgentNotifications.head
			oldAgentNotification.recipients.size should be (1)
			oldAgentNotification.recipients.head should be (staff4.asSsoUser)
			oldAgentNotification.entities should be (Seq(rel1, rel3))
		}

	}
}

