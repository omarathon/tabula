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
		studentCourseDetails.sprCode returns studentSprCode
		studentCourseDetails.route returns studentRoute

		student.studentCourseDetails returns JArrayList(studentCourseDetails)

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
		meetingThisYearPoint.requiredFromWeek = 1
		meetingThisYearPoint.pointType = MonitoringPointType.Meeting
		meetingThisYearPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
		meetingThisYearPoint.relationshipService = mock[RelationshipService]
		meetingThisYearPoint.relationshipService
			.getStudentRelationshipTypeById(meeting.relationship.relationshipType.id) returns Option(meeting.relationship.relationshipType)
		meetingThisYearPoint.meetingFormats = Seq(meeting.format)
	}

	@Test
	def willBeCreatedMeetingNotApproved() { new StudentFixture {
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateMeetingNotApproved() { new StudentFixture {
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		service.updateCheckpointsForMeeting(meeting)
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait NoSuchStudent extends StudentFixture {
		val thisMeeting = new MeetingRecord
		thisMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		val thisMeetingRelationship = StudentRelationship(agent, tutorRelationshipType, studentCourseDetails.sprCode)
		thisMeetingRelationship.profileService = mock[ProfileService]
		thisMeetingRelationship.profileService.getStudentBySprCode(studentCourseDetails.sprCode) returns None
		thisMeeting.relationship = thisMeetingRelationship
	}

	@Test
	def willBeCreatedNoSuchStudent() { new NoSuchStudent {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateNoSuchStudent() { new NoSuchStudent {
		service.updateCheckpointsForMeeting(thisMeeting)
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait NonMeetingPoint extends Year2PointSetFixture {
		val normalThisYearPoint = new MonitoringPoint
		normalThisYearPoint.pointSet = year2PointSet
		year2PointSet.points = JArrayList(normalThisYearPoint)

		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
	}

	@Test
	def willBeCreatedNonMeetingPoint() { new NonMeetingPoint {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateNonMeetingPoint() { new NonMeetingPoint {
		service.updateCheckpointsForMeeting(meeting)
		there was no (service.monitoringPointDao).getCheckpoint(any[MonitoringPoint], any[String])
		there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait MeetingPointWrongRelationship extends Year2PointSetFixture {
		val meetingThisYearPoint = new MonitoringPoint
		meetingThisYearPoint.pointSet = year2PointSet
		year2PointSet.points = JArrayList(meetingThisYearPoint)
		meetingThisYearPoint.pointType = MonitoringPointType.Meeting
		meetingThisYearPoint.meetingRelationships = Seq(supervisorRelationshipType)
		meetingThisYearPoint.relationshipService = mock[RelationshipService]
		meetingThisYearPoint.relationshipService.getStudentRelationshipTypeById(supervisorRelationshipType.id) returns Option(supervisorRelationshipType)

		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
	}

	@Test
	def willBeCreatedMeetingPointWrongRelationship() { new MeetingPointWrongRelationship {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateMeetingPointWrongRelationship() { new MeetingPointWrongRelationship {
		service.updateCheckpointsForMeeting(meeting)
		there was no (service.monitoringPointDao).getCheckpoint(any[MonitoringPoint], any[String])
		there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait MeetingPointWrongFormat extends Year2PointSetFixture {
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
	}

	@Test
	def willBeCreatedMeetingPointWrongFormat() { new MeetingPointWrongFormat {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateMeetingPointWrongFormat() { new MeetingPointWrongFormat {
		service.updateCheckpointsForMeeting(meeting)
		there was no (service.monitoringPointDao).getCheckpoint(any[MonitoringPoint], any[String])
		there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait MeetingExistingCheckpoint extends Year2PointSetFixture {
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
	}

	@Test
	def willBeCreatedMeetingExistingCheckpoint() { new MeetingExistingCheckpoint {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateMeetingExistingCheckpoint() { new MeetingExistingCheckpoint {
		service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was no (service.termService).getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear])
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait MeetingBeforePoint extends Year2PointSetFixture {
		val meetingThisYearPoint = new MonitoringPoint
		meetingThisYearPoint.pointSet = year2PointSet
		year2PointSet.points = JArrayList(meetingThisYearPoint)
		meetingThisYearPoint.validFromWeek = 4
		meetingThisYearPoint.requiredFromWeek = 5
		meetingThisYearPoint.pointType = MonitoringPointType.Meeting
		meetingThisYearPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
		meetingThisYearPoint.relationshipService = mock[RelationshipService]
		meetingThisYearPoint.relationshipService
			.getStudentRelationshipTypeById(meeting.relationship.relationshipType.id) returns Option(meeting.relationship.relationshipType)
		meetingThisYearPoint.meetingFormats = Seq(meeting.format)
		service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None

		val meetingDate = dateTime(2013, 1, 7)
		service.termService.getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear) returns 2

		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		meeting.meetingDate = meetingDate
	}

	@Test
	def willBeCreatedMeetingBeforePoint() { new MeetingBeforePoint {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateMeetingBeforePoint() { new MeetingBeforePoint {
		service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait MeetingAfterPoint extends Year2PointSetFixture {
		val meetingThisYearPoint = new MonitoringPoint
		meetingThisYearPoint.pointSet = year2PointSet
		year2PointSet.points = JArrayList(meetingThisYearPoint)
		meetingThisYearPoint.validFromWeek = 4
		meetingThisYearPoint.requiredFromWeek = 5
		meetingThisYearPoint.pointType = MonitoringPointType.Meeting
		meetingThisYearPoint.meetingRelationships = Seq(meeting.relationship.relationshipType)
		meetingThisYearPoint.relationshipService = mock[RelationshipService]
		meetingThisYearPoint.relationshipService
			.getStudentRelationshipTypeById(meeting.relationship.relationshipType.id) returns Option(meeting.relationship.relationshipType)
		meetingThisYearPoint.meetingFormats = Seq(meeting.format)
		service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None

		val meetingDate = dateTime(2013, 1, 7)
		service.termService.getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear) returns 6

		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		meeting.meetingDate = meetingDate
	}

	@Test
	def willBeCreatedMeetingAfterPoint() { new MeetingAfterPoint {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateMeetingAfterPoint() { new MeetingAfterPoint {
		service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait ValidPointNotEnoughMeetingsApproved extends ValidYear2PointFixture {
		meetingThisYearPoint.meetingQuantity = 2

		service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
		service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
		service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

		val otherMeeting = new MeetingRecord
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))

		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		meeting.meetingDate = DateTime.now
	}

	@Test
	def willBeCreatedValidPointNotEnoughMeetingsApproved() { new ValidPointNotEnoughMeetingsApproved {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateValidPointNotEnoughMeetingsApproved() { new ValidPointNotEnoughMeetingsApproved {
		service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait ValidPointNotEnoughMeetingsCorrectFormat extends ValidYear2PointFixture {
		meetingThisYearPoint.meetingQuantity = 2

		service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
		service.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear)) returns 2
		service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

		val otherMeeting = new MeetingRecord
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		otherMeeting.format = MeetingFormat.Email

		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		meeting.meetingDate = DateTime.now
	}

	@Test
	def willBeCreatedValidPointNotEnoughMeetingsCorrectFormat() { new ValidPointNotEnoughMeetingsCorrectFormat {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateValidPointNotEnoughMeetingsCorrectFormat() { new ValidPointNotEnoughMeetingsCorrectFormat {
		service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was one (service.termService).getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(year2PointSet.academicYear))
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait ValidPointNotEnoughMeetingsValidWeek extends ValidYear2PointFixture {
		// needs 2 meetings
		meetingThisYearPoint.meetingQuantity = 2
		val beforeDate = dateTime(2013, 1, 7).minusDays(7)
		val meetingDate = dateTime(2013, 1, 7)

		service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
		service.termService.getAcademicWeekForAcademicYear(beforeDate, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
		service.termService.getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
		service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

		val otherMeeting = new MeetingRecord
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		otherMeeting.format = MeetingFormat.FaceToFace

		// only 1 is valid
		otherMeeting.meetingDate = beforeDate
		meeting.meetingDate = meetingDate

		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)

		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
	}

	@Test
	def willBeCreatedValidPointNotEnoughMeetingsValidWeek() { new ValidPointNotEnoughMeetingsValidWeek {
		service.willCheckpointBeCreated(meeting) should be (right = false)
	}}

	@Test
	def updateValidPointNotEnoughMeetingsValidWeek() { new ValidPointNotEnoughMeetingsValidWeek {
		service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was two (service.termService).getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear)
		there was one (service.termService).getAcademicWeekForAcademicYear(beforeDate, year2PointSet.academicYear)
		there was no (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	trait ValidPointValidMeetings extends ValidYear2PointFixture {
		// needs 2 meetings
		meetingThisYearPoint.meetingQuantity = 2
		val beforeDate = dateTime(2013, 1, 7).minusDays(7)
		val meetingDate = dateTime(2013, 1, 7).minusDays(7)

		service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
		service.termService.getAcademicWeekForAcademicYear(beforeDate, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
		service.termService.getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
		service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

		val otherMeeting = new MeetingRecord
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		otherMeeting.format = MeetingFormat.FaceToFace

		// 2 are valid
		otherMeeting.meetingDate = meetingDate
		meeting.meetingDate = meetingDate

		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)
	}

	@Test
	def willBeCreatedValidPointValidMeetings() { new ValidPointValidMeetings {
		// the current meeting is pending, but if it were approved it should create the checkpoint
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		service.willCheckpointBeCreated(meeting) should be (right = true)
	}}

	@Test
	def updateValidPointValidMeetings() { new ValidPointValidMeetings {
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Approved))
		val createdCheckpoints = service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was three (service.termService).getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear)
		there was one (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		createdCheckpoints.size should be (1)
		createdCheckpoints.head.state should be (MonitoringCheckpointState.Attended)
		createdCheckpoints.head.studentCourseDetail should be (studentCourseDetails)
		createdCheckpoints.head.point should be (meetingThisYearPoint)
		createdCheckpoints.head.updatedBy should be (agentMember.universityId)
	}}

	trait ValidPointMeetingNotApprovedButCreatedByAgent extends ValidYear2PointFixture {
		// needs 2 meetings
		meetingThisYearPoint.meetingQuantity = 2
		val beforeDate = dateTime(2013, 1, 7).minusDays(7)
		val meetingDate = dateTime(2013, 1, 7)

		service.monitoringPointDao.getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode) returns None
		service.termService.getAcademicWeekForAcademicYear(beforeDate, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek - 1
		service.termService.getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear) returns meetingThisYearPoint.validFromWeek
		service.relationshipService.getRelationships(meetingThisYearPoint.meetingRelationships.head, studentCourseDetails.sprCode) returns Seq(meetingRelationship)

		val otherMeeting = new MeetingRecord
		otherMeeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		otherMeeting.format = MeetingFormat.FaceToFace
		otherMeeting.relationship = meetingRelationship
		otherMeeting.creator = agentMember

		// 2 are valid
		otherMeeting.meetingDate = meetingDate
		meeting.meetingDate = meetingDate

		service.meetingRecordDao.list(meetingRelationship) returns Seq(meeting, otherMeeting)
		meeting.approvals = JArrayList(Fixtures.meetingRecordApproval(MeetingApprovalState.Pending))
		meeting.creator = agentMember
	}

	@Test
	def willBeCreatedValidPointMeetingNotApprovedButCreatedByAgent() { new ValidPointMeetingNotApprovedButCreatedByAgent {
		service.willCheckpointBeCreated(meeting) should be (right = true)
	}}

	@Test
	def updateValidPointMeetingNotApprovedButCreatedByAgent() { new ValidPointMeetingNotApprovedButCreatedByAgent {
		val createdCheckpoints = service.updateCheckpointsForMeeting(meeting)
		there was one (service.monitoringPointDao).getCheckpoint(meetingThisYearPoint, studentCourseDetails.sprCode)
		there was three (service.termService).getAcademicWeekForAcademicYear(meetingDate, year2PointSet.academicYear)
		there was one (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
		createdCheckpoints.size should be (1)
		createdCheckpoints.head.state should be (MonitoringCheckpointState.Attended)
		createdCheckpoints.head.studentCourseDetail should be (studentCourseDetails)
		createdCheckpoints.head.point should be (meetingThisYearPoint)
		createdCheckpoints.head.updatedBy should be (agentMember.universityId)
	}}

}