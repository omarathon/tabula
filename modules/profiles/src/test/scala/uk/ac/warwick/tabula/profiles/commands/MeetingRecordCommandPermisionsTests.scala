package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Mockito, CurrentUser}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.RelationshipType.{Supervisor, PersonalTutor}
import scala.Some
import org.springframework.validation.BindingResult

trait MeetingRecordCommandPermisionsTests extends Mockito{
  val ps = mock[ProfileService]

  var student:StudentMember = _
  var creator: StaffMember = _
  var relationship: StudentRelationship = _
  var supervisorRelationship: StudentRelationship = _
  var tutorMeeting: MeetingRecord = _
  var supervisorMeeting: MeetingRecord = _
  var approval: MeetingRecordApproval = _
  val user = mock[CurrentUser]
  user.universityId returns("9876543")
  ps.getStudentBySprCode("1170836/1") returns (Some(student))

  @Before
  def setUp {
    creator = {
      val m = new StaffMember("9876543")
      m.userId = "staffmember"
      m
    }

    student = {
      val m = new StudentMember("1170836")
      m.userId = "studentmember"
      m
    }

    relationship = {
      val relationship = StudentRelationship("Professor A Tutor", PersonalTutor, "1170836/1")
      relationship.profileService = ps
      relationship
    }

    supervisorRelationship = {
      val relationship = StudentRelationship("Professor A Tutor", Supervisor, "1170836/1")
      relationship.profileService = ps
      relationship
    }


    tutorMeeting = {
      val mr = new MeetingRecord
      mr.creator = creator
      mr.relationship = relationship
      mr
    }

    supervisorMeeting = {
      val mr = new MeetingRecord
      mr.creator = creator
      mr.relationship = supervisorRelationship
      mr
    }


  }
  trait StubCommand{
    def emit: Notification[MeetingRecord] = null

    def onBind(result: BindingResult) {}

    val meeting: MeetingRecord = null
  }
}
