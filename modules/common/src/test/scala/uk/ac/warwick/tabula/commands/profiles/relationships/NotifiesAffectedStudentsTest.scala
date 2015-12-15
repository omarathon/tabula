package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.data.model.notifications.profiles.{BulkNewAgentRelationshipNotification, BulkOldAgentRelationshipNotification, BulkStudentRelationshipNotification}
import uk.ac.warwick.tabula.data.model.{Notification, StaffMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class NotifiesAffectedStudentsTest extends TestBase with Mockito {

	trait Environment {
		val Tutor = StudentRelationshipType("1", "tutor", "tutor", "tutee")

		val dept = Fixtures.department("in", "IT Services")
		val relType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val student1 = Fixtures.student("0000001", "student1", dept)
		val student2 = Fixtures.student("0000002", "student2", dept)
		val student3 = Fixtures.student("0000003", "student3", dept)
		val student4 = Fixtures.student("0000004", "student4", dept)
		val student5 = Fixtures.student("0000005", "student5", dept)
		val student6 = Fixtures.student("0000006", "student6", dept)
		val student7 = Fixtures.student("0000007", "student7", dept)

		val staff1 = Fixtures.staff("1000001", "staff1", dept)
		val staff2 = Fixtures.staff("1000002", "staff2", dept)
		val staff3 = Fixtures.staff("1000003", "staff3", dept)
		val staff4 = Fixtures.staff("1000004", "staff4", dept)
		val staff5 = Fixtures.staff("1000005", "staff5", dept)

		val rel1 = StudentRelationship(staff1, relType, student1)
		val rel2 = StudentRelationship(staff1, relType, student2)
		val rel3 = StudentRelationship(staff2, relType, student3)

		var relService = smartMock[RelationshipService]
		var profService = smartMock[ProfileService]

		profService.getMemberByUniversityId("1000001", false, false) returns Some(staff1)
		profService.getMemberByUniversityId("1000002", false, false) returns Some(staff2)
		profService.getMemberByUniversityId("1000004", false, false) returns Some(staff4)
		profService.getMemberByUniversityId("1000005", false, false) returns Some(staff5)

		val cmd = new NotifiesAffectedStudents with RelationshipChangingCommand {
			val department = dept
			val relationshipType = relType
			var relationshipService = relService
			var profileService = profService
			val apparentUser: User = smartMock[User]

			notifyNewAgent = true
			notifyOldAgents = true
			notifyStudent = true
		}

	}

	@Test
	def testEmitNewRelationship: Unit = {
		new Environment {
			// one modified relationship and no old agents - it must be a new relationship, not replacing anything
			val oldAgents = Seq[StaffMember]()
			val modifiedRelationship = rel1
			val change = new StudentRelationshipChange(oldAgents, rel1)

			val relChanges = Seq[StudentRelationshipChange](change)

			val notifications: Seq[Notification[StudentRelationship, Unit]] = cmd.emit(relChanges)

			notifications.size should be(2)

			val newAgentNotifications = notifications.filter(_.isInstanceOf[BulkNewAgentRelationshipNotification])
			newAgentNotifications.size should be(1)

			val studentNotifications = notifications.filter(_.isInstanceOf[BulkStudentRelationshipNotification]).asInstanceOf[Seq[BulkStudentRelationshipNotification]]
			studentNotifications.size should be(1)
			studentNotifications.head.oldAgents.isEmpty should be(true)
		}
	}

	@Test
	def testEmitChangedRelationship {
		new Environment {

			// one modified relationship and one old agent
			val oldAgents = Seq(staff2)
			val change = new StudentRelationshipChange(oldAgents, rel1)

			val relChanges = Seq[StudentRelationshipChange](change)

			val notifications = cmd.emit(relChanges)

			notifications.size should be(3)

			val newAgentNotifications = notifications.filter(_.isInstanceOf[BulkNewAgentRelationshipNotification])
			newAgentNotifications.size should be(1)
			newAgentNotifications.head.recipients should be(List(staff1.asSsoUser))

			val studentNotifications = notifications.filter(_.isInstanceOf[BulkStudentRelationshipNotification]).asInstanceOf[Seq[BulkStudentRelationshipNotification]]
			studentNotifications.size should be(1)
			studentNotifications.head.oldAgents.head should be(staff2)
			studentNotifications.head.recipients should be(List(student1.asSsoUser))

			val oldAgentNotifications: Seq[BulkOldAgentRelationshipNotification] = notifications.filter(_.isInstanceOf[BulkOldAgentRelationshipNotification]).asInstanceOf[Seq[BulkOldAgentRelationshipNotification]]
			oldAgentNotifications.size should be(1)
			val oldAgentNotification: BulkOldAgentRelationshipNotification = oldAgentNotifications.head
			oldAgentNotification.recipients should be(oldAgents.map(_.asSsoUser))

		}
	}

	@Test
	def testEmitTwoChangedRelationships {
		new Environment {
			// two modified relationships, each with one old agent
			val oldAgentsForRel1 = Seq(staff4)
			val change = new StudentRelationshipChange(oldAgentsForRel1, rel1)

			val oldAgentsForRel3 = Seq(staff5)
			val change2 = new StudentRelationshipChange(oldAgentsForRel3, rel3)

			val relChanges = Seq[StudentRelationshipChange](change, change2)

			val notifications = cmd.emit(relChanges)

			notifications.size should be(6)

			val newAgentNotifications = notifications.filter(_.isInstanceOf[BulkNewAgentRelationshipNotification])
			newAgentNotifications.size should be(2)
			newAgentNotifications.head.recipients should be(List(staff1.asSsoUser))

			val studentNotifications = notifications.filter(_.isInstanceOf[BulkStudentRelationshipNotification]).asInstanceOf[Seq[BulkStudentRelationshipNotification]]
			studentNotifications.size should be(2)
			val studentNotificationForStudent1 = studentNotifications.filter(_.recipients.contains(student1.asSsoUser)).head
			studentNotificationForStudent1.oldAgents should be(Seq(staff4))

			val oldAgentNotifications = notifications.filter(_.isInstanceOf[BulkOldAgentRelationshipNotification]).asInstanceOf[Seq[BulkOldAgentRelationshipNotification]]
			oldAgentNotifications.size should be(2)
			val oldAgentNotificationForStaff4: BulkOldAgentRelationshipNotification = oldAgentNotifications.filter(_.recipients.contains(staff4.asSsoUser)).head
			oldAgentNotificationForStaff4.oldAgents should be(Seq(staff4))
		}
	}

	@Test
	def testEmitModifiedRelationshipsWithSameOldAgent {
		new Environment {

			// two modified relationships with same old agent
			val oldAgentsForRel1 = Seq(staff4)
			val change = new StudentRelationshipChange(oldAgentsForRel1, rel1)

			val oldAgentsForRel3 = Seq(staff4)
			val change2 = new StudentRelationshipChange(oldAgentsForRel3, rel3)

			val relChanges = Seq[StudentRelationshipChange](change, change2)

			val notifications = cmd.emit(relChanges)

			notifications.size should be (5)

			val oldAgentNotifications = notifications.filter(_.isInstanceOf[BulkOldAgentRelationshipNotification]).asInstanceOf[Seq[BulkOldAgentRelationshipNotification]]
			oldAgentNotifications.size should be (1)

			val oldAgentNotification = oldAgentNotifications.head
			oldAgentNotification.recipients.size should be (1)
			oldAgentNotification.recipients.head should be (staff4.asSsoUser)
			oldAgentNotification.entities should be (Seq(rel1, rel3))
		}

	}
}
