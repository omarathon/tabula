package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.tabula.data.model._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.notifications.{BulkStudentRelationshipNotification, BulkOldAgentRelationshipNotification}

class BulkRelationshipChangeNotificationTest extends TestBase with Mockito {

	val profiles = smartMock[ProfileService]
	val service = smartMock[RelationshipService]

	val Tutor = StudentRelationshipType("1", "tutor", "tutor", "tutee")

	val student = new StudentMember()

	val agent0 = Fixtures.member(userType=MemberUserType.Staff ,universityId="0")
	val agent1 = Fixtures.member(userType=MemberUserType.Staff ,universityId="1")
	val agent2 = Fixtures.member(userType=MemberUserType.Staff ,universityId="2")

	val rel0 = relationship(agent0, new DateTime(2013,1,20, 12,0))
	val rel1 = relationship(agent1, new DateTime(2013,1,10, 12,0))
	val rel2 = relationship(agent2, new DateTime(2013,1,1, 12,0))

	val rels = Seq(rel0, rel1, rel2)

	rels.foreach { rel =>
		rel.profileService = profiles
	}

	trait Environment {
		// set the service up to return all three relationships the student
		service.getRelationships(Tutor, student) returns (rels)

		// set the service up to return the appropriate previous relationship
		service.getPreviousRelationship(rel0) returns None
		service.getPreviousRelationship(rel1) returns Some(rel0)
		service.getPreviousRelationship(rel2) returns Some(rel1)

		profiles.getMemberByUniversityId("1") returns (Some(agent1))
	}

	@Test
	def studentNotification() {
		new Environment {
			val notification = new BulkStudentRelationshipNotification
			notification.relationshipService = service
			notification.addItems(Seq(rel1))
			notification.newAgent should be (None) // rel1 is ended, so there should be no new agent

			val notification2 = new BulkStudentRelationshipNotification
			notification2.relationshipService = service
			notification2.addItems(Seq(rel2))
			notification2.newAgent.get.universityId should be ("2")	// rel2 is not ended, so the new agent for the notification is the agent of the relationship
		}
	}

	// create a relationship between spr code "student/1" and the given agent, of relationship type Tutor
	def relationship(agent: Member, startDate: DateTime): MemberStudentRelationship = {
		// end the previous relationship
		if (agent.universityId == "2") rel1.endDate = startDate // we're creating rel2 - end rel1
		else if (agent.universityId == "1") rel0.endDate = startDate // we're creating rel1 - end rel0

		// create the new relationship
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
