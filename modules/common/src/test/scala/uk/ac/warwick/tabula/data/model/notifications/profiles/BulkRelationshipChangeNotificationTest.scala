package uk.ac.warwick.tabula.data.model.notifications.profiles

import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType, Notification}

class BulkRelationshipChangeNotificationTest extends TestBase {

	val agent = Fixtures.staff("1234567")
	agent.firstName = "Tutor"
	agent.lastName = "Name"

	val student = Fixtures.student("7654321")
	student.firstName = "Student"
	student.lastName = "Name"

	val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

	val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student)

	@Test def titleStudent() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new BulkStudentRelationshipNotification, currentUser.apparentUser, relationship)
		notification.title should be ("Personal tutor allocation change")
	}

	@Test def titleOldTutor() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new BulkOldAgentRelationshipNotification, currentUser.apparentUser, relationship)
		notification.title should be ("Change to personal tutees")
	}

	@Test def titleNewTutor() = withUser("0672089", "cuscav") {
		val notification = Notification.init(new BulkNewAgentRelationshipNotification, currentUser.apparentUser, relationship)
		notification.title should be ("Allocation of new personal tutees")
	}

}
