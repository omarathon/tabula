package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Mockito, AppContextTestBase, CurrentUser}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.MeetingFormat.FaceToFace
import scala.Some
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending

trait MeetingRecordTests extends AppContextTestBase with Mockito {
	val aprilFool = dateTime(2013, DateTimeConstants.APRIL)
	val marchHare = dateTime(2013, DateTimeConstants.MARCH).toLocalDate

	val ps = mock[ProfileService]

	var student:StudentMember = _
	var creator: StaffMember = _
	var relationship: StudentRelationship = _
	var meeting: MeetingRecord = _

	val user = mock[CurrentUser]
	user.universityId returns("9876543")

	@Before
	def setUp {
		creator = transactional { tx =>
			val m = new StaffMember("9876543")
			m.userId = "staffmember"
			session.save(m)
			m
		}

		student = transactional { tx =>
			val m = new StudentMember("1170836")
			m.userId = "studentmember"
			session.save(m)
			m
		}

		relationship = transactional { tx =>
			val relationship = StudentRelationship("Professor A Tutor", PersonalTutor, "1170836/1")
			relationship.profileService = ps
			ps.getStudentBySprCode("1170836/1") returns (Some(student))

			session.save(relationship)
			relationship
		}

		meeting = {
			val mr = new MeetingRecord
			mr.creator = creator
			mr.relationship = relationship
			mr.title = "A title"
			mr.format = FaceToFace
			mr.meetingDate = aprilFool
			mr.description = "Lovely words"

			val approval = new MeetingRecordApproval
			approval.approver = relationship.studentMember.get
			approval.creationDate = aprilFool
			approval.state = Pending
			approval.meetingRecord = mr

			mr.approvals.add(approval)
			mr
		}
	}
}
