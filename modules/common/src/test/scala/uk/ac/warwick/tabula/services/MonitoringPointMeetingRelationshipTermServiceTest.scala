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
		var relationshipService = mock[RelationshipService]
		val termService = mock[TermService]
	}

	val now = dateTime(2013, 1, 7)

	trait StudentFixture {
		val service = new AbstractMonitoringPointMeetingRelationshipTermService with ServiceTestSupport

		val academicYear2012 = AcademicYear(2012)
		val academicYear2013 = AcademicYear(2013)

		val student = Fixtures.student("1234")
		val studentRoute = Fixtures.route("a100")
		val studentCourseDetails = student.freshStudentCourseDetails(0)
		studentCourseDetails.route = studentRoute

		val agent = "agent"
		val agentMember = Fixtures.staff(agent, agent)

		val tutorRelationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")
		val supervisorRelationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		val meetingRelationship = StudentRelationship(agent, tutorRelationshipType, studentCourseDetails.sprCode)
		meetingRelationship.profileService = mock[ProfileService]
		meetingRelationship.profileService.getStudentBySprCode(studentCourseDetails.sprCode) returns Option(student)
		meetingRelationship.profileService.getMemberByUniversityId(agent) returns Option(agentMember)

		val meeting = new MeetingRecord
		meeting.relationship = meetingRelationship
		meeting.format = MeetingFormat.FaceToFace
		meeting.creator = Fixtures.student("student", "student")
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
		studentCourseDetails.addStudentCourseYearDetails(studentCourseYear1)
		studentCourseDetails.addStudentCourseYearDetails(studentCourseYear2)
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
	def willBeCreatedMeetingNotApproved() {
		new Year2PointSetFixture {
			val thisMeeting = new MeetingRecord
			thisMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
			val thisMeetingRelationship = StudentRelationship(agent, tutorRelationshipType, studentCourseDetails.sprCode)
			thisMeetingRelationship.profileService = mock[ProfileService]
			thisMeetingRelationship.profileService.getStudentBySprCode(studentCourseDetails.sprCode) returns Some(student)
			thisMeeting.relationship = thisMeetingRelationship
			service.willCheckpointBeCreated(thisMeeting) should be (right = false)
		}
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
	def willBeCreatedNoSuchStudent() {
		new StudentFixture {
			val thisMeeting = new MeetingRecord
			thisMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			val thisMeetingRelationship = StudentRelationship(agent, tutorRelationshipType, studentCourseDetails.sprCode)
			thisMeetingRelationship.profileService = mock[ProfileService]
			thisMeetingRelationship.profileService.getStudentBySprCode(studentCourseDetails.sprCode) returns None
			thisMeeting.relationship = thisMeetingRelationship
			service.willCheckpointBeCreated(thisMeeting) should be (right = false)

		}
	}

	@Test
	def updateNoSuchStudent() {
		new StudentFixture {
			val thisMeeting = new MeetingRecord
			thisMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			val thisMeetingRelationship = StudentRelationship(agent, tutorRelationshipType, studentCourseDetails.sprCode)
			thisMeetingRelationship.profileService = mock[ProfileService]
			thisMeetingRelationship.profileService.getStudentBySprCode(studentCourseDetails.sprCode) returns None
			thisMeeting.relationship = thisMeetingRelationship
			service.updateCheckpointsForMeeting(thisMeeting)
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def willBeCreatedNonMeetingPoint() = withFakeTime(now) {
		new Year2PointSetFixture {

			val normalThisYearPoint = new MonitoringPoint
			normalThisYearPoint.pointSet = year2PointSet
			year2PointSet.points = JArrayList(normalThisYearPoint)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.willCheckpointBeCreated(meeting) should be (right = false)
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
	def willBeCreatedMeetingPointWrongRelationship() = withFakeTime(now) {
		new Year2PointSetFixture {
			val meetingThisYearPoint = new MonitoringPoint
			meetingThisYearPoint.pointSet = year2PointSet
			year2PointSet.points = JArrayList(meetingThisYearPoint)
			meetingThisYearPoint.pointType = MonitoringPointType.Meeting
			meetingThisYearPoint.meetingRelationships = Seq(supervisorRelationshipType)
			meetingThisYearPoint.relationshipService = mock[RelationshipService]
			meetingThisYearPoint.relationshipService.getStudentRelationshipTypeById(supervisorRelationshipType.id) returns Option(supervisorRelationshipType)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.willCheckpointBeCreated(meeting) should be (right = false)
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
	def willBeCreatedMeetingPointWrongFormat() = withFakeTime(now) {
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
			service.willCheckpointBeCreated(meeting) should be (right = false)
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
	def willBeCreatedMeetingExistingCheckpoint() = withFakeTime(now) {
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
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns Option(new MonitoringCheckpoint)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.willCheckpointBeCreated(meeting) should be (right = false)
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
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns Option(new MonitoringCheckpoint)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
			there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def willBeCreatedMeetingFuturePoint() = withFakeTime(now) {
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
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.willCheckpointBeCreated(meeting) should be (right = false)
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
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
			there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def willBeCreatedValidPointNotEnoughMeetingsApproved() = withFakeTime(now) {
		new ValidYear2PointFixture {

			meetingThisYearPoint.meetingQuantity = 2

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.willCheckpointBeCreated(meeting) should be (right = false)
		}
	}

	@Test
	def updateValidPointNotEnoughMeetingsApproved() = withFakeTime(now) {
		new ValidYear2PointFixture {

			meetingThisYearPoint.meetingQuantity = 2

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
			there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def willBeCreatedValidPointNotEnoughMeetingsCorrectFormat() = withFakeTime(now) {
		new ValidYear2PointFixture {

			meetingThisYearPoint.meetingQuantity = 2

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.Email

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.willCheckpointBeCreated(meeting) should be (right = false)
		}
	}

	@Test
	def updateValidPointNotEnoughMeetingsCorrectFormat() = withFakeTime(now) {
		new ValidYear2PointFixture {

			meetingThisYearPoint.meetingQuantity = 2

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.Email

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
			there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def willBeCreatedValidPointNotEnoughMeetingsValidWeek() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.FaceToFace

			// only 1 is valid
			otherMeeting.meetingDate = beforeNow
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.willCheckpointBeCreated(meeting) should be (right = false)
		}
	}

	@Test
	def updateValidPointNotEnoughMeetingsValidWeek() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.FaceToFace

			// only 1 is valid
			otherMeeting.meetingDate = beforeNow
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
			there was two (service.termService).getAcademicWeekForAcademicYear(now, year2PointSet.academicYear)
			there was one (service.termService).getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear)
			there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		}
	}

	@Test
	def willBeCreatedValidPointValidMeetings() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.FaceToFace

			// 2 are valid
			otherMeeting.meetingDate = now
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			// the current meeting is pending, but if it were approved it should create the checkpoint
			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
			service.willCheckpointBeCreated(meeting) should be (right = true)
		}
	}

	@Test
	def updateValidPointValidMeetings() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			otherMeeting.format = MeetingFormat.FaceToFace

			// 2 are valid
			otherMeeting.meetingDate = now
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			val createdCheckpoints = service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
			there was three (service.termService).getAcademicWeekForAcademicYear(now, year2PointSet.academicYear)
			there was one (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
			createdCheckpoints.size should be (1)
			createdCheckpoints.head.state should be (MonitoringCheckpointState.Attended)
			createdCheckpoints.head.studentCourseDetail should be (studentCourseDetails)
			createdCheckpoints.head.point should be (meetingThisYearPoint)
			createdCheckpoints.head.updatedBy should be (agentMember.universityId)
		}
	}

	@Test
	def willBeCreatedValidPointMeetingNotApprovedButCreatedByAgent() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
			otherMeeting.format = MeetingFormat.FaceToFace
			otherMeeting.relationship = meetingRelationship
			otherMeeting.creator = agentMember

			// 2 are valid
			otherMeeting.meetingDate = now
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
			meeting.creator = agentMember
			service.willCheckpointBeCreated(meeting) should be (right = true)
		}
	}

	@Test
	def updateValidPointMeetingNotApprovedButCreatedByAgent() = withFakeTime(now) {
		new ValidYear2PointFixture {

			// needs 2 meetings
			meetingThisYearPoint.meetingQuantity = 2
			val beforeNow = dateTime(2013, 1, 7).minusDays(7)

			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(beforeNow, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

			val otherMeeting = new MeetingRecord
			otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
			otherMeeting.format = MeetingFormat.FaceToFace
			otherMeeting.relationship = meetingRelationship
			otherMeeting.creator = agentMember

			// 2 are valid
			otherMeeting.meetingDate = now
			meeting.meetingDate = now

			service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
			meeting.creator = agentMember
			val createdCheckpoints = service.updateCheckpointsForMeeting(meeting)
			there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
			there was three (service.termService).getAcademicWeekForAcademicYear(now, year2PointSet.academicYear)
			there was one (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
			createdCheckpoints.size should be (1)
			createdCheckpoints.head.state should be (MonitoringCheckpointState.Attended)
			createdCheckpoints.head.studentCourseDetail should be (studentCourseDetails)
			createdCheckpoints.head.point should be (meetingThisYearPoint)
			createdCheckpoints.head.updatedBy should be (agentMember.universityId)
		}
	}

	@Test
	def formatsNotEnoughMeetings() = withFakeTime(now) {
		new ValidYear2PointFixture {
			meetingThisYearPoint.meetingQuantity = 3
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)
			service.meetingRecordDao.list(meeting.relationship) returns Seq(meeting)
			meeting.meetingDate = now

			service.formatsThatWillCreateCheckpoint(meeting.relationship).size should be (0)
		}
	}

	@Test
	def formatsEnoughMeetings() = withFakeTime(now) {
		new ValidYear2PointFixture {
			meetingThisYearPoint.meetingQuantity = 2
			service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
			service.termService.getAcademicWeekForAcademicYear(now, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
			meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
			service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)
			service.meetingRecordDao.list(meeting.relationship) returns Seq(meeting)
			meeting.meetingDate = now

			val formats = service.formatsThatWillCreateCheckpoint(meeting.relationship)
			formats.size should be (meetingThisYearPoint.meetingFormats.size)

		}
	}

}
