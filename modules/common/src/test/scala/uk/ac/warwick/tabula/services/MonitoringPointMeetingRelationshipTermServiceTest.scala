package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent, MonitoringPointDaoComponent, MonitoringPointDao}
import uk.ac.warwick.tabula.{AcademicYear, TestBase, Fixtures, Mockito}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointType, MonitoringPoint, MonitoringPointSet, MonitoringCheckpoint}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model._
import org.joda.time.DateTime
import org.mockito.Matchers

class MonitoringPointMeetingRelationshipTermServiceTest extends TestBase with Mockito {
	trait ServiceTestSupport extends MonitoringPointDaoComponent with MeetingRecordDaoComponent
		with RelationshipServiceComponent with TermServiceComponent {

		val monitoringPointDao = mock[MonitoringPointDao]
		val meetingRecordDao = mock[MeetingRecordDao]
		val relationshipService = mock[RelationshipService]
		val termService = mock[TermService]
	}

	val now = dateTime(2013, 1, 7)

	trait StudentFixture {
		val service = new AbstractMonitoringPointMeetingRelationshipTermService with ServiceTestSupport

		val academicYear2012 = AcademicYear(2012)
		val academicYear2013 = AcademicYear(2013)

		val student = mock[StudentMember]
		val studentSprCode = "1234/1"
		student.universityId returns "1234"
		val studentRoute = Fixtures.route("a100")

		val studentCourseDetails = mock[StudentCourseDetails]
		studentCourseDetails.scjCode returns studentSprCode
		studentCourseDetails.route returns studentRoute

		student.mostSignificantCourseDetails returns Option(studentCourseDetails)

		val agent = "agent"

		val tutorRelationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")
		val supervisorRelationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		val meetingRelationship = StudentRelationship(agent, tutorRelationshipType, studentSprCode)
		meetingRelationship.profileService = mock[ProfileService]
		meetingRelationship.profileService.getStudentBySprCode(studentSprCode) returns Option(student)

		val meeting = new MeetingRecord
		meeting.relationship = meetingRelationship
		meeting.format = MeetingFormat.FaceToFace
	}

	trait StudentYear1Fixture extends StudentFixture {
		val studentCourseYear1 = new StudentCourseYearDetails
		studentCourseYear1.yearOfStudy = 1
		studentCourseYear1.academicYear = academicYear2012
	}

	trait StudentYear2Fixture extends StudentFixture {
		val studentCourseYear1 = new StudentCourseYearDetails
		studentCourseYear1.yearOfStudy = 1
		studentCourseYear1.academicYear = academicYear2012
		val studentCourseYear2 = new StudentCourseYearDetails
		studentCourseYear2.yearOfStudy = 2
		studentCourseYear2.academicYear = academicYear2013
		studentCourseDetails.studentCourseYearDetails returns JArrayList(studentCourseYear1, studentCourseYear2)
	}

	trait Year2PointSetFixture extends StudentYear2Fixture {
		val year1PointSet = new MonitoringPointSet
		year1PointSet.academicYear = studentCourseYear1.academicYear
		year1PointSet.route = studentRoute
		year1PointSet.year = 1

		val year2PointSet = new MonitoringPointSet
		year2PointSet.academicYear = studentCourseYear2.academicYear
		year2PointSet.route = studentRoute
		year2PointSet.year = 2

		service.monitoringPointDao.findMonitoringPointSets(studentRoute, academicYear2012) returns Seq(year1PointSet)
		service.monitoringPointDao.findMonitoringPointSets(studentRoute, academicYear2013) returns Seq(year2PointSet)
	}

	trait ValidYear2PointFixture extends Year2PointSetFixture {
		val meetingThisYearPoint = new MonitoringPoint
		meetingThisYearPoint.pointSet = year2PointSet
		year2PointSet.points = JArrayList(meetingThisYearPoint)
		meetingThisYearPoint.validFromWeek = 1
		meetingThisYearPoint.pointType = MonitoringPointType.Meeting
		meetingThisYearPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
		meetingThisYearPoint.relationshipService = mock[RelationshipService]
		meetingThisYearPoint.relationshipService
			.getStudentRelationshipTypeById(meeting.relationship.relationshipType.id) returns Option(meeting.relationship.relationshipType)
		meetingThisYearPoint.meetingFormats = Seq(meeting.format)
	}

	@Test
	def updateMeetingNotApproved() {
		new StudentFixture {
			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
			service.updateCheckpointsForMeeting(meeting)
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateNoSuchStudent() {
		new StudentFixture {
			val thisMeeting = new MeetingRecord
			thisMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			val thisMeetingRelationship = StudentRelationship(agent, tutorRelationshipType, studentSprCode)
			thisMeetingRelationship.profileService = mock[ProfileService]
			thisMeetingRelationship.profileService.getStudentBySprCode(studentSprCode) returns None
			thisMeeting.relationship = thisMeetingRelationship
			service.updateCheckpointsForMeeting(thisMeeting)
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateNonMeetingPoint() = withFakeTime(now) {
		new Year2PointSetFixture {

			val normalThisYearPoint = new MonitoringPoint
			normalThisYearPoint.pointSet = year2PointSet
			year2PointSet.points = JArrayList(normalThisYearPoint)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was no (service.monitoringPointDao).getCheckpoint(any[MonitoringPoint], any[String])
			there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateMeetingPointWrongRelationship() = withFakeTime(now) {
		new Year2PointSetFixture {
			val meetingThisYearPoint = new MonitoringPoint
			meetingThisYearPoint.pointSet = year2PointSet
			year2PointSet.points = JArrayList(meetingThisYearPoint)
			meetingThisYearPoint.pointType = MonitoringPointType.Meeting
			meetingThisYearPoint.meetingRelationships = Seq(supervisorRelationshipType)
			meetingThisYearPoint.relationshipService = mock[RelationshipService]
			meetingThisYearPoint.relationshipService.getStudentRelationshipTypeById(supervisorRelationshipType.id) returns Option(supervisorRelationshipType)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was no (service.monitoringPointDao).getCheckpoint(any[MonitoringPoint], any[String])
			there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateMeetingPointWrongFormat() = withFakeTime(now) {
		new Year2PointSetFixture {
			val meetingThisYearPoint = new MonitoringPoint
			meetingThisYearPoint.pointSet = year2PointSet
			year2PointSet.points = JArrayList(meetingThisYearPoint)
			meetingThisYearPoint.pointType = MonitoringPointType.Meeting
			meetingThisYearPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
			meetingThisYearPoint.relationshipService = mock[RelationshipService]
			meetingThisYearPoint.relationshipService
				.getStudentRelationshipTypeById(meeting.relationship.relationshipType.id) returns Option(meeting.relationship.relationshipType)
			meetingThisYearPoint.meetingFormats = Seq(MeetingFormat.Email)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was no (service.monitoringPointDao).getCheckpoint(any[MonitoringPoint], any[String])
			there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateMeetingExistingCheckpoint() = withFakeTime(now) {
		new Year2PointSetFixture {
			val meetingThisYearPoint = new MonitoringPoint
			meetingThisYearPoint.pointSet = year2PointSet
			year2PointSet.points = JArrayList(meetingThisYearPoint)
			meetingThisYearPoint.pointType = MonitoringPointType.Meeting
			meetingThisYearPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
			meetingThisYearPoint.relationshipService = mock[RelationshipService]
			meetingThisYearPoint.relationshipService
				.getStudentRelationshipTypeById(meeting.relationship.relationshipType.id) returns Option(meeting.relationship.relationshipType)
			meetingThisYearPoint.meetingFormats = Seq(meeting.format)
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentSprCode) returns Option(new MonitoringCheckpoint)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentSprCode)
			there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateMeetingFuturePoint() = withFakeTime(now) {
		new Year2PointSetFixture {
			val meetingThisYearPoint = new MonitoringPoint
			meetingThisYearPoint.pointSet = year2PointSet
			year2PointSet.points = JArrayList(meetingThisYearPoint)
			meetingThisYearPoint.validFromWeek = 4
			meetingThisYearPoint.pointType = MonitoringPointType.Meeting
			meetingThisYearPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
			meetingThisYearPoint.relationshipService = mock[RelationshipService]
			meetingThisYearPoint.relationshipService
				.getStudentRelationshipTypeById(meeting.relationship.relationshipType.id) returns Option(meeting.relationship.relationshipType)
			meetingThisYearPoint.meetingFormats = Seq(meeting.format)
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentSprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentSprCode)
			there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateValidPointNotEnoughMeetingsApproved() = withFakeTime(now) {
		new ValidYear2PointFixture {

			meetingThisYearPoint.meetingQuantity = 2

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentSprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, student.universityId) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentSprCode)
			there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateValidPointNotEnoughMeetingsCorrectFormat() = withFakeTime(now) {
		new ValidYear2PointFixture {

			meetingThisYearPoint.meetingQuantity = 2

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentSprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, student.universityId) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.Email

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentSprCode)
			there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateValidPointNotEnoughMeetingsValidWeek() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentSprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, student.universityId) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.FaceToFace

			// only 1 is valid
			otherMeeting.meetingDate = beforeNow
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentSprCode)
			there was two (service.termService).getAcademicWeekForAcademicYear(now, year2PointSet.academicYear)
			there was one (service.termService).getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear)
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def updateValidPointValidMeetings() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentSprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, student.universityId) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.FaceToFace

			// 2 are valid
			otherMeeting.meetingDate = now
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			val createdCheckpoints = service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentSprCode)
			there was three (service.termService).getAcademicWeekForAcademicYear(now, year2PointSet.academicYear)
			there was one (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
			createdCheckpoints.size should be (1)
			createdCheckpoints.head.state should be (MonitoringCheckpointState.Attended)
			createdCheckpoints.head.studentCourseDetail should be (studentCourseDetails)
			createdCheckpoints.head.point should be (meetingThisYearPoint)
			createdCheckpoints.head.updatedBy should be (agent)
		}
	}

}
