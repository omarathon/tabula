package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.notifications.profiles.BulkStudentRelationshipNotification
import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.tabula.data.model._
import org.joda.time.DateTime

class BulkRelationshipChangeNotificationTest extends TestBase with Mockito {

	val profiles = smartMock[ProfileService]
	val service = smartMock[RelationshipService]

	val Tutor = StudentRelationshipType("1", "tutor", "tutor", "tutee")

	val student = new StudentMember()

	val agent0 = Fixtures.member(userType=MemberUserType.Staff ,universityId="0")
	val agent1 = Fixtures.member(userType=MemberUserType.Staff ,universityId="1")
	val agent2 = Fixtures.member(userType=MemberUserType.Staff ,universityId="2")
	val agent3 = Fixtures.member(userType=MemberUserType.Staff ,universityId="3")

	val rel0 = relationship(agent0, new DateTime(2013,1,20, 12,0))
	val rel1 = relationship(agent1, new DateTime(2013,1,10, 12,0))
	val rel2 = relationship(agent2, new DateTime(2013,1,1, 12,0))
	val rel3 = relationship(agent3, new DateTime(2013,1,1, 12,0))

	val rels = Seq(rel0, rel1, rel2)

	rels.foreach { rel =>
		rel.profileService = profiles
	}

	trait Environment {
		// set the service up to return all three relationships the student
		service.getRelationships(Tutor, student) returns rels

		profiles.getMemberByUniversityId("0") returns Some(agent0)
		profiles.getMemberByUniversityId("1") returns Some(agent1)
		profiles.getMemberByUniversityId("2") returns Some(agent2)
		profiles.getMemberByUniversityId("3") returns Some(agent3)
	}

	@Test
	def studentNotificationRelEnded() {
		new Environment {
			val notification = new BulkStudentRelationshipNotification
			notification.relationshipService = service
			notification.profileService = profiles
			notification.addItems(Seq(rel1))

			notification.oldAgentIds.value = Seq(agent2.universityId)
			notification.oldAgents should be (Seq(agent2))

			notification.newAgents.isEmpty should be {true} // rel1 is ended, so there should be no new agent
		}
	}

	@Test
	def studentNotificationNewAgent() {
		new Environment {
			val notification = new BulkStudentRelationshipNotification
			notification.relationshipService = service
			notification.profileService = profiles

			notification.addItems(Seq(rel2))
			notification.newAgents.head.universityId should be ("2")	// rel2 is not ended, so the new agent for the notification is the agent of the relationship
		}
	}

	@Test
	def studentNotificationMultipleAgentsAddedAndRemoved() {
		new Environment {
			val notification = new BulkStudentRelationshipNotification
			notification.relationshipService = service
			notification.profileService = profiles

			notification.addItems(Seq(rel2, rel3))
			notification.oldAgentIds.value = Seq(agent0.universityId, agent1.universityId)

			notification.newAgents.size should be (2)
			notification.oldAgents.size should be (2)

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
