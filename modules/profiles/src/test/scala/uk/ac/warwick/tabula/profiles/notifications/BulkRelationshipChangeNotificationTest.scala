package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.tabula.data.model._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.notifications.BulkOldAgentRelationshipNotification
import scala.Some

class BulkRelationshipChangeNotificationTest extends TestBase with Mockito {

	val profiles = smartMock[ProfileService]
	val service = smartMock[RelationshipService]

	val Tutor = StudentRelationshipType("1", "tutor", "tutor", "tutee")

	val student = new StudentMember()

	val agent988 = Fixtures.member(userType=MemberUserType.Staff ,universityId="988")
	val agent999 = Fixtures.member(userType=MemberUserType.Staff ,universityId="999")
	val agent977 = Fixtures.member(userType=MemberUserType.Staff ,universityId="977")

	val rels = Seq(
		relationship(agent988, new DateTime(2013,1,10, 12,0)),
		relationship(agent999, new DateTime(2013,1,20, 12,0)),
		relationship(agent977, new DateTime(2013,1,1, 12,0))
	)

	rels.foreach { rel =>
		rel.profileService = profiles
	}

	val rel1 = rels(1)

	@Test
	def oldAgentNotification() {
		service.getRelationships(Tutor, student) returns (rels)
		service.getPreviousRelationship(rel1) returns (rels.headOption)

		profiles.getMemberByUniversityId("988") returns (Some(agent988))

		val notification = new BulkOldAgentRelationshipNotification
		notification.relationshipService = service
		notification.addItems(Seq(rel1))
		notification.oldAgent.get.universityId should be ("988")
	}

	def relationship(agent: Member, startDate: DateTime) = {
		val rel = new MemberStudentRelationship()
		val courseDetails = new StudentCourseDetails()
		courseDetails.sprCode = "student/1"
		rel.studentCourseDetails = courseDetails
		rel.relationshipType = Tutor
		rel.startDate = startDate
		rel.agentMember = agent
		rel
	}
}
