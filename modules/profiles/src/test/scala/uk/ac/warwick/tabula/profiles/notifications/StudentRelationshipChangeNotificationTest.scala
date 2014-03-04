package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.profiles.TutorFixture
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.notifications.{StudentRelationshipChangeToNewAgentNotification, StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}


class StudentRelationshipChangeNotificationTest extends TestBase with Mockito with FreemarkerRendering {

	val relationshipService = smartMock[RelationshipService]

	def createNewTutorNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]) = {
		val n = new StudentRelationshipChangeToNewAgentNotification
		n.agent = actor
		n.addItems(Seq(relationship))
		n
	}

	def createOldTutorNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]) = {
		val n = new StudentRelationshipChangeToOldAgentNotification
		n.agent = actor
		n.addItems(Seq(relationship))
		n
	}

	def createTuteeNotification(relationship:StudentRelationship, actor:User, oldTutor: Option[Member]) = {
		val n = new StudentRelationshipChangeToStudentNotification
		n.agent = actor
		n.addItems(Seq(relationship))
		n
	}

	@Test
	def urlIsProfilePage():Unit = new TutorFixture {
		val n = createNewTutorNotification(relationship, actor, Some(oldTutor))
		n.relationshipService = relationshipService
		n.url should be("/profiles/view/student")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new TutorFixture {
		val n = createOldTutorNotification(relationship, actor, Some(oldTutor))
		n.relationshipService = relationshipService
		when (n.relationshipService.getPreviousRelationship(relationship)) thenReturn Some(relationshipOld)
		n.recipients should be (Seq(recipient))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new TutorFixture {
		val n = createTuteeNotification(relationship, actor,  Some(oldTutor))
		n.relationshipService = relationshipService
		when (n.relationshipService.getPreviousRelationship(relationship)) thenReturn Some(relationshipOld)
		n.content.template should be ("/WEB-INF/freemarker/notifications/student_change_relationship_notification.ftl")
		n.content.model("student") should be (Some(student))
		n.content.model("path") should be ("/profiles/view/student")
		n.content.model("oldAgent") should be (Some(oldTutor))
		n.content.model("newAgent") should be (Some(newTutor))
	}
}
