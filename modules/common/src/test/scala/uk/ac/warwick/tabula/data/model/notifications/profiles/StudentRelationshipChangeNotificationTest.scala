package uk.ac.warwick.tabula.data.model.notifications.profiles

import uk.ac.warwick.tabula.data.model.{Notification, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class StudentRelationshipChangeNotificationTest extends TestBase {

	val agent = Fixtures.staff("1234567")
	agent.firstName = "Tutor"
	agent.lastName = "Name"

	val student = Fixtures.student("7654321")
	student.firstName = "Student"
	student.lastName = "Name"

	val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

	val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student)

	@Test def titleStudent() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new StudentRelationshipChangeToStudentNotification, currentUser.apparentUser, relationship)
		notification.title should be ("Personal tutor allocation change")
	}

	@Test def titleOldTutor() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new StudentRelationshipChangeToOldAgentNotification, currentUser.apparentUser, relationship)
		notification.title should be ("Change to personal tutees")
	}

	@Test def titleNewTutor() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new StudentRelationshipChangeToNewAgentNotification, currentUser.apparentUser, relationship)
		notification.title should be ("Allocation of new personal tutees")
	}

}
