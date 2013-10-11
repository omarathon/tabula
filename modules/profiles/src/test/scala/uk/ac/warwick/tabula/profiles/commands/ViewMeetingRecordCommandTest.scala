package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent, ProfileService, ProfileServiceComponent}
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}

class ViewMeetingRecordCommandTest extends TestBase with Mockito {

	@Test
	def listsAllMeetingsForRequestedStudentAndCurrentUser {
		withUser("test"){
			val studentCourseDetails = new StudentCourseDetails()
			val requestor = new StaffMember()
			val relationship = new StudentRelationship()
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			val meeting = new MeetingRecord
			val command = new ViewMeetingRecordCommandInternal(studentCourseDetails, Some(requestor), relationshipType)
				with RelationshipServiceComponent with ProfileServiceComponent with MeetingRecordDaoComponent{
				var profileService = mock[ProfileService]
				var relationshipService = mock[RelationshipService]
				var meetingRecordDao = mock[MeetingRecordDao]
			}

			// these are the calls we expect the applyInternal method to make
			command.relationshipService.getRelationships(relationshipType, studentCourseDetails.sprCode) returns Seq(relationship)
			command.meetingRecordDao.list(Set(relationship), requestor) returns  Seq(meeting)

			command.applyInternal()  should be(Seq(meeting))
		}
	}

}
