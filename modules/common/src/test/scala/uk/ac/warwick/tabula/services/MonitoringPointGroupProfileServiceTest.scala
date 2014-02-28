package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.{MonitoringPointDaoComponent, SmallGroupDao, SmallGroupDaoComponent, MonitoringPointDao}
import uk.ac.warwick.tabula.{AcademicYear, TestBase, Fixtures, Mockito}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringPointType, MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventOccurrence, SmallGroupEventAttendance, SmallGroupSet, SmallGroup}

class MonitoringPointGroupProfileServiceTest extends TestBase with Mockito {

	val mockProfileService = mock[ProfileService]
	val mockMonitoringPointService = mock[MonitoringPointService]

	trait ServiceTestSupport extends SmallGroupServiceComponent with SmallGroupDaoComponent
	with ProfileServiceComponent with  MonitoringPointServiceComponent with MonitoringPointDaoComponent {

		val monitoringPointDao = mock[MonitoringPointDao]

		val monitoringPointService = mockMonitoringPointService
		val profileService = mockProfileService
		val smallGroupService = mock[SmallGroupService]
		val smallGroupDao = mock[SmallGroupDao]


	}

	trait StudentFixture {
		val service = new AbstractMonitoringPointGroupProfileService with ServiceTestSupport

		val profileService = mock[ProfileService]

		val academicYear2012 = AcademicYear(2012)
		val academicYear2013 = AcademicYear(2013)

		val student = Fixtures.student("1234")
		val studentRoute = Fixtures.route("a100")
		val studentCourseDetails = student.mostSignificantCourseDetails.get
		studentCourseDetails.route = studentRoute

		val agent = "agent"
		val agentMember = Fixtures.staff(agent, agent)

		val tutorRelationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")
		val supervisorRelationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		val relationship = ExternalStudentRelationship(agent, tutorRelationshipType, student)

		val group = new SmallGroup
		val groupSet = new SmallGroupSet
		groupSet.academicYear = AcademicYear(2013)
		group.groupSet = groupSet

		val event = new SmallGroupEvent(group)

		val occurrence = new SmallGroupEventOccurrence
		occurrence.event = event
		occurrence.week = 1

		val attendance = new SmallGroupEventAttendance
		attendance.occurrence = occurrence
		attendance.universityId = student.universityId
		attendance.state = AttendanceState.Attended
		occurrence.attendance.add(attendance)

		mockProfileService.getMemberByUniversityId(student.universityId) returns Option(student)

	}

	trait StudentYear2Fixture extends StudentFixture {
		val studentCourseYear1 = studentCourseDetails.latestStudentCourseYearDetails
		studentCourseYear1.yearOfStudy = 1
		studentCourseYear1.academicYear = academicYear2012

		val studentCourseYear2 = Fixtures.studentCourseYearDetails(academicYear2013)
		studentCourseYear2.yearOfStudy = 2
		studentCourseDetails.addStudentCourseYearDetails(studentCourseYear1)
		studentCourseDetails.addStudentCourseYearDetails(studentCourseYear2)


	}

	trait Year2PointSetFixture extends StudentYear2Fixture {
		val year2PointSet = new MonitoringPointSet
		year2PointSet.academicYear = studentCourseYear2.academicYear
		year2PointSet.route = studentRoute
		year2PointSet.year = 2

		service.monitoringPointService.getPointSetForStudent(student, academicYear2013) returns Option(year2PointSet)

	}

	trait ValidYear2PointFixture extends Year2PointSetFixture {
		val groupThisYearPoint = new MonitoringPoint
		groupThisYearPoint.pointSet = year2PointSet
		year2PointSet.points = JArrayList(groupThisYearPoint)
		groupThisYearPoint.validFromWeek = 1
		groupThisYearPoint.requiredFromWeek = 1
		groupThisYearPoint.pointType = MonitoringPointType.SmallGroup
		groupThisYearPoint.smallGroupEventModules = Seq()
		groupThisYearPoint.smallGroupEventQuantity = 1
		groupThisYearPoint.relationshipService = mock[RelationshipService]
		groupThisYearPoint.moduleAndDepartmentService = mock[ModuleAndDepartmentService]

		mockMonitoringPointService.getCheckpoint(student, groupThisYearPoint ) returns None
		mockMonitoringPointService.studentAlreadyReportedThisTerm(student, groupThisYearPoint) returns false

	}


	@Test
	def updatesCheckpoint() { new ValidYear2PointFixture {

		service.getCheckpointsForAttendance(Seq(attendance)).size should be (1)

//		service.updateCheckpointsForAttendance(Seq(attendance))
//		there was one (service.monitoringPointDao).saveOrUpdate(any[MonitoringCheckpoint])
	}}

	@Test
	def wrongWeek() { new ValidYear2PointFixture {

		occurrence.week = 2
		service.getCheckpointsForAttendance(Seq(attendance)).size should be (0)

	}}

	@Test
	def wrongPointType() { new ValidYear2PointFixture {

		groupThisYearPoint.pointType = MonitoringPointType.Meeting
		service.getCheckpointsForAttendance(Seq(attendance)).size should be (0)

	}}

	@Test
	def checkpointAlreadyExists() { new ValidYear2PointFixture {

		mockMonitoringPointService.getCheckpoint(student, groupThisYearPoint ) returns Option(Fixtures.monitoringCheckpoint(groupThisYearPoint, student, AttendanceState.Attended))
		service.getCheckpointsForAttendance(Seq(attendance)).size should be (0)

	}}


}
