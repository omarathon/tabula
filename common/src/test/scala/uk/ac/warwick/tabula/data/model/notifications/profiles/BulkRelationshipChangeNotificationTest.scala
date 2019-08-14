package uk.ac.warwick.tabula.data.model.notifications.profiles

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._

class BulkRelationshipChangeNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  val agent: StaffMember = Fixtures.staff("1234567")
  agent.firstName = "Tutor"
  agent.lastName = "Name"

  val agentReplacement: StaffMember = Fixtures.staff("1234568")
  agentReplacement.firstName = "Other"
  agentReplacement.lastName = "Tutor"

  val student: StudentMember = Fixtures.student("7654321")
  student.firstName = "Student"
  student.lastName = "Name"

  val student2: StudentMember = Fixtures.student("7654322")
  student2.firstName = "Student2"
  student2.lastName = "Name2"

  val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

  val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)
  val relationship2: StudentRelationship = StudentRelationship(agent, relationshipType, student2, DateTime.now)

  @Test def titleStudent() = withUser("0672089", "cuscav") {
    val notification = Notification.init(new BulkStudentRelationshipNotification, currentUser.apparentUser, Seq(relationship, relationship2))
    notification.title should be("Personal tutor allocation change")
  }

  @Test def titleContentOldTutor() = withUser("0672089", "cuscav") {
    val notification = Notification.init(new BulkOldAgentRelationshipNotification, currentUser.apparentUser, Seq(relationship, relationship2))
    notification.title should be("Change to personal tutees")

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """You are no longer assigned as personal tutor to the following students:
        |
        |* Student Name
        |* Student2 Name2
        |
        |""".stripMargin
    )
  }

  @Test def titleContentOldTutorComplex() = withUser("0672089", "cuscav") {
    val notification = Notification.init(new BulkOldAgentRelationshipNotification, currentUser.apparentUser, Seq(relationship, relationship2))
    notification.title should be("Change to personal tutees")

    notification.scheduledDate = new DateTime(2019, DateTimeConstants.JANUARY, 15, 15, 30, 0, 0)
    notification.previouslyScheduledDate = new DateTime(2019, DateTimeConstants.JANUARY, 30, 15, 30, 0, 0)
    relationship2.replacedBy = StudentRelationship(agentReplacement, relationshipType, student, DateTime.now)

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """As of 15:30 Tue 15th January 2019 you will be no longer assigned as personal tutor to the following students:
        |
        |* Student Name
        |* Student2 Name2 (new personal tutor Other Tutor)
        |
        |This change was originally scheduled to happen at 15:30 Wed 30th January 2019.
        |""".stripMargin
    )
  }

  @Test def titleContentNewTutor() = withUser("0672089", "cuscav") {
    val notification = Notification.init(new BulkNewAgentRelationshipNotification, currentUser.apparentUser, Seq(relationship, relationship2))
    notification.title should be("Allocation of new personal tutees")

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """You have been assigned as personal tutor to the following students:
        |
        |* Student Name
        |* Student2 Name2
        |
        |""".stripMargin
    )
  }

  @Test def titleContentNewTutorComplex() = withUser("0672089", "cuscav") {
    val notification = Notification.init(new BulkNewAgentRelationshipNotification, currentUser.apparentUser, Seq(relationship, relationship2))
    notification.title should be("Allocation of new personal tutees")

    notification.scheduledDate = new DateTime(2019, DateTimeConstants.JANUARY, 15, 15, 30, 0, 0)
    notification.previouslyScheduledDate = new DateTime(2019, DateTimeConstants.JANUARY, 30, 15, 30, 0, 0)
    relationship2.replacesRelationships.add(StudentRelationship(agentReplacement, relationshipType, student, DateTime.now))

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """As of 15:30 Tue 15th January 2019 you will be assigned as personal tutor to the following students:
        |
        |* Student Name
        |* Student2 Name2 (previous personal tutor Other Tutor)
        |
        |This change was originally scheduled to happen at 15:30 Wed 30th January 2019.
        |""".stripMargin
    )
  }

  val profiles: ProfileService = smartMock[ProfileService]
  val service: RelationshipService = smartMock[RelationshipService]

  val Tutor = StudentRelationshipType("1", "tutor", "tutor", "tutee")

  val agent0: Member = Fixtures.member(userType = MemberUserType.Staff, universityId = "0")
  val agent1: Member = Fixtures.member(userType = MemberUserType.Staff, universityId = "1")
  val agent2: Member = Fixtures.member(userType = MemberUserType.Staff, universityId = "2")
  val agent3: Member = Fixtures.member(userType = MemberUserType.Staff, universityId = "3")

  val rel0: MemberStudentRelationship = relationship(agent0, new DateTime(2013, 1, 20, 12, 0))
  val rel1: MemberStudentRelationship = relationship(agent1, new DateTime(2013, 1, 10, 12, 0))
  val rel2: MemberStudentRelationship = relationship(agent2, new DateTime(2013, 1, 1, 12, 0))
  val rel3: MemberStudentRelationship = relationship(agent3, new DateTime(2013, 1, 1, 12, 0))

  val rels = Seq(rel0, rel1, rel2)

  rels.foreach { rel =>
    rel.profileService = profiles
  }

  private trait Environment {
    // set the service up to return all three relationships the student
    service.getRelationships(Tutor, student) returns rels

    profiles.getMemberByUniversityId("0") returns Some(agent0)
    profiles.getMemberByUniversityId("1") returns Some(agent1)
    profiles.getMemberByUniversityId("2") returns Some(agent2)
    profiles.getMemberByUniversityId("3") returns Some(agent3)
  }

  @Test
  def studentNotificationNewAgent() {
    new Environment {
      val notification = new BulkStudentRelationshipNotification
      notification.relationshipService = service
      notification.profileService = profiles

      notification.addItems(Seq(rel2))
      notification.entities.flatMap(_.agentMember).head.universityId should be("2") // rel2 is not ended, so the new agent for the notification is the agent of the relationship
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
      notification.newAgentIds.value = Seq(agent2.universityId, agent3.universityId)
      notification.newAgents.size should be(2)
      notification.oldAgents.size should be(2)

      val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
      notificationContent should be(
        """You have been assigned 2 Staff and 3 Staff as your tutors.
          |
          |0 Staff and 1 Staff are no longer your tutors.""".stripMargin
      )
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
