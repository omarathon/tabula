package uk.ac.warwick.tabula.coursework.commands.departments

import collection.JavaConversions._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.AuditEventQueryMethods
import uk.ac.warwick.tabula.services.AbstractIndexService
import uk.ac.warwick.tabula.services.AssignmentMembershipService

class FeedbackReportCommandTest extends AppContextTestBase with Mockito with ReportWorld {
	
	var auditIndexService = mock[AuditEventQueryMethods]
	
//	auditIndexService.submissionForStudent(any[Assignment], any[User]) answers {_ match {
//		case(a, u) => {
//			val assignment = a.asInstanceOf[Assignment]
//			val user = u.asInstanceOf[User]
//			auditEvents.filter(event => {event.userId == user.getUserId() && event.assignmentId.get == assignment.id})
//		}
//		case _ => Seq()
//	}}
	
	auditIndexService.submissionForStudent(any[Assignment], any[User]) answers {argsObj => {		
		val args = argsObj.asInstanceOf[Array[_]]
		val assignment = args(0).asInstanceOf[Assignment]
		val user = args(1).asInstanceOf[User]
		auditEvents.filter(event => {event.userId == user.getUserId() && event.assignmentId.get == assignment.id})
	}}
	
	auditIndexService.publishFeedbackForStudent(any[Assignment], any[User]) answers {argsObj => {
		val args = argsObj.asInstanceOf[Array[_]]
		val assignment = args(0).asInstanceOf[Assignment]
		val user = args(1).asInstanceOf[User]
		auditEvents.filter(event => {event.students.contains(user.getWarwickId) && event.assignmentId.get == assignment.id})
	}}

	var assignmentMembershipService = mock[AssignmentMembershipService]
	assignmentMembershipService.determineMembershipUsers(any[Assignment]) answers { assignmentObj =>
		val assignment = assignmentObj.asInstanceOf[Assignment]
		val studentIds = assignment.members.includeUsers
		val users = studentIds.map{userId =>
			val userOne = new User(userId)
			userOne.setWarwickId(userId)
			userOne
		}.toList
		users
	}
	

	@Test
	def simpleGetSubmissionTest() {
		val userOne = new User(idFormat(1))
		userOne.setWarwickId(idFormat(1))
		
		var submissions = auditIndexService.submissionForStudent(assignmentOne, userOne)
		submissions.size should be (1)

	}
	
	
	
	
	@Test
	def simpleGetFeedbackTest() {
		val userOne = new User(idFormat(1))
		userOne.setWarwickId(idFormat(1))
		
		val publishes = auditIndexService.publishFeedbackForStudent(assignmentOne, userOne)
		publishes.size should be (1)
		
	}
	

	/*
	
	@Test
	def feedbackCountsTest() {
		val feedbackReportCommand = new FeedbackReportCommand(department)
		val feedbackCount = feedbackReportCommand.getFeedbackCounts(assignmentOne) 
		
		feedbackCount should be (10,0)
	}
	*/
	
	
}
