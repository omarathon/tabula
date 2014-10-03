package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.tabula.data.model.notifications.{StudentRelationshipChangeToNewAgentNotification, StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}

class StudentRelationshipChangeNotificationTest extends TestBase with Mockito with FreemarkerRendering with TutorFixture {

	def createNewTutorNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]) = {
		val n = new StudentRelationshipChangeToNewAgentNotification
		n.agent = actor
		n.oldAgentIds.value = Seq(oldTutor.get.universityId)
		n.addItems(Seq(relationship))
		n
	}

	def createOldTutorNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]) = {
		val n = new StudentRelationshipChangeToOldAgentNotification
		n.profileService = profileService

		n.agent = actor
		n.oldAgentIds.value = Seq(oldTutor.get.universityId)
		n.addItems(Seq(relationship))
		n
	}

	def createTuteeNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]) = {
		val n = new StudentRelationshipChangeToStudentNotification
		n.profileService = smartMock[ProfileService]
		n.agent = actor
		n.oldAgentIds.value = Seq(oldTutor.get.universityId)
		n.addItems(Seq(relationship))
		n
	}

	@Test
	def urlIsProfilePage():Unit = new TutorFixture {
		val n = createNewTutorNotification(relationship, actor, Some(oldTutor))
		n.url should be("/profiles/view/student")
		n.urlTitle should be ("view the student profile for Test Student")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new TutorFixture {
		val n = createOldTutorNotification(relationship, actor, Some(oldTutor))
		n.recipients should be (Seq(recipient))
		n.urlTitle should be ("view the student profile for Test Student")
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new TutorFixture {
		val n = createTuteeNotification(relationship, actor,  Some(oldTutor))
		n.profileService = profileService
		n.content.template should be ("/WEB-INF/freemarker/notifications/student_change_relationship_notification.ftl")
		n.content.model("student") should be (Some(student))
		n.content.model("path") should be ("/profiles/view/student")
		n.content.model("newAgents") should be (Seq(newTutor))
		n.urlTitle should be ("view your student profile")
	}

}
