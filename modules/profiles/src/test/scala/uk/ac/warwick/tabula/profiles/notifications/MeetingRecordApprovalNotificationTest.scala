package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.views.ScalaBeansWrapper
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.ScopelessPermission
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.web.views.UrlMethodModel

class MeetingRecordApprovalNotificationTest extends TestBase with Mockito {
	
	val securityService = mock[SecurityService]
	securityService.can(any[CurrentUser], any[ScopelessPermission]) returns true
	securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget]) returns true
	
	val profileService = mock[ProfileService]
	
	val freeMarkerConfig = newFreemarkerConfiguration
	freeMarkerConfig.getObjectWrapper.asInstanceOf[ScalaBeansWrapper].securityService = securityService
	
	val urlMethodModel = new UrlMethodModel
	urlMethodModel.context = "/profiles"
	urlMethodModel.toplevelUrl = "https://tabula.warwick.ac.uk"
	
	freeMarkerConfig.setSharedVariable("url", urlMethodModel)
	
	@Test def itWorks {
		val department = Fixtures.department("es", "Engineering")
		
		val agent = Fixtures.staff("1234567", "estaff", department)
		agent.firstName = "Karen"
		agent.lastName = "Bradbury"
		
		val student = Fixtures.student("1218503", "esustu", department)
		student.mostSignificantCourse = Fixtures.studentCourseDetails(student, department)
		student.mostSignificantCourse.sprCode = "1218503/1"
			
		profileService.getStudentBySprCode("1218503/1") returns Some(student)
		
		withUser(agent.userId, agent.universityId) {
			val meeting = new MeetingRecord
			meeting.id = "fd269caf-c739-4a44-8f3e-27e79110c73d"
			meeting.creator = agent
			meeting.title = "End of term progress meeting"
			meeting.meetingDate = new DateTime(2013, DateTimeConstants.DECEMBER, 5, 12, 0, 0, 0)
			
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
			meeting.relationship = StudentRelationship(agent.universityId, relationshipType, student.mostSignificantCourseDetails.get.sprCode)
			meeting.relationship.profileService = profileService
			
			val notification = new MeetingRecordApprovalNotification(meeting, "create")
			notification.freemarker = freeMarkerConfig
			
			notification.content should be (
"""This record of your personal tutor meeting has been created by Karen Bradbury:

End of term progress meeting on 5 December 2013

Please visit https://tabula.warwick.ac.uk/profiles/view/1218503?meeting=fd269caf-c739-4a44-8f3e-27e79110c73d to approve or reject it.""")
		}
	}

}