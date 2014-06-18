package uk.ac.warwick.tabula.profiles.web.controllers

import org.joda.time.DateTime
import org.mockito.Matchers
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{TermService, SmallGroupService, ProfileService, SecurityService}
import uk.ac.warwick.util.termdates.Term

class ViewProfileControllerTest extends TestBase with Mockito{

	val controller = new ViewProfileByStudentController
	// need to have a security service defined or we'll get a NPE in PermissionsCheckingMethods.restricted()
	controller.securityService = mock[SecurityService]
	controller.smallGroupService = mock[SmallGroupService]
	controller.profileService = mock[ProfileService]
	controller.termService = mock[TermService]
	
	val member = new StudentMember()
	val courseDetails = new StudentCourseDetails()
	courseDetails.mostSignificant = true
	member.attachStudentCourseDetails(courseDetails)
	member.mostSignificantCourse = courseDetails

	@Test(expected=classOf[ItemNotFoundException])
	def throwsNonStudentStaff() {
		withUser("test") {
			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)
			val emeritusMember = new EmeritusMember()
			val cmd = controller.viewProfileCommand(emeritusMember)
		}
	}

	@Test
	def getsProfileCommand() {
		withUser("test") {
			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)
			val cmd = controller.viewProfileCommand(member)
			cmd.value should be(member)
		}
	}

	@Test def getMeetingRecordCommand() {
		withUser("test") {
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			controller.smallGroupService.findSmallGroupsByStudent(currentUser.apparentUser) returns (Nil)
			val cmd = controller.getViewMeetingRecordCommand(Some(courseDetails), relationshipType)
			cmd should not be(None)

			val staffMember = new StaffMember
			val notACmd = controller.getViewMeetingRecordCommand(None, relationshipType)
			notACmd should be(None)
		}
	}
	
	
	@Test def getFilterMeetingsByYear() {
		withUser("test") {
			controller.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2012))) returns 5
			controller.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2011))) returns Term.WEEK_NUMBER_BEFORE_START
			controller.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2013))) returns Term.WEEK_NUMBER_AFTER_END
			
			val meeting1 = new MeetingRecord
			meeting1.meetingDate = dateTime(2012, 1, 7)
			
			val meeting2 = new MeetingRecord
			meeting2.meetingDate = dateTime(2012, 1, 9)
			
			val allMeetings = Seq(meeting1, meeting2)
			
			var filteredMeetings = controller.filterMeetingsByYear(allMeetings, new AcademicYear(2012))
			filteredMeetings.length should be (2)

			filteredMeetings = controller.filterMeetingsByYear(allMeetings, new AcademicYear(2011))
			filteredMeetings.length should be (0)

			filteredMeetings = controller.filterMeetingsByYear(Seq(), new AcademicYear(2013))
			filteredMeetings.length should be (0)
		}
	}

}