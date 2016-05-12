package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.{DateTime, DateTimeConstants, Interval, LocalDate}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint.Settings
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, _}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._
import scala.collection.mutable

class MonitoringPointMigrationCommandTest extends TestBase with Mockito {

	val thisDepartment = Fixtures.department("its")
	val thisAcademicYear = AcademicYear(2013)
	val thisRoute = Fixtures.route("a100")
	thisRoute.adminDepartment = thisDepartment
	val tutorRelationshipType = new StudentRelationshipType
	tutorRelationshipType.id = "tutor"
	val module = Fixtures.module("its01")
	module.id = "its01"
	val assignment = Fixtures.assignment("its01")
	assignment.id = "its01"
	val mockMonitoringPointService = smartMock[MonitoringPointService]
	mockMonitoringPointService.studentAlreadyReportedThisTerm(any[StudentMember], any[MonitoringPoint]) returns false
	val mockAttendanceMonitoringService = smartMock[AttendanceMonitoringService]
	val mockRelationshipService = smartMock[RelationshipService]
	mockRelationshipService.getStudentRelationshipTypeById(tutorRelationshipType.id) returns Option(tutorRelationshipType)
	val mockModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
	mockModuleAndDepartmentService.getModuleById(module.id) returns Option(module)
	val mockAssignmentService = smartMock[AssessmentService]
	mockAssignmentService.getAssignmentById(assignment.id) returns Option(assignment)
	val mockTermService = smartMock[TermService]
	mockTermService.getAcademicWeeksForYear(thisAcademicYear.dateInTermOne) returns Seq((new Integer(1), new Interval(
		new LocalDate(thisAcademicYear.startYear, DateTimeConstants.NOVEMBER, 1).toDateTimeAtStartOfDay,
		new LocalDate(thisAcademicYear.startYear, DateTimeConstants.NOVEMBER, 8).toDateTimeAtStartOfDay
	)))

	val student1 = Fixtures.student("1234")
	val student2 = Fixtures.student("2345")

	val set = new MonitoringPointSet
	set.route = thisRoute
	set.academicYear = thisAcademicYear
	set.createdDate = DateTime.now.minusYears(1)
	set.updatedDate = DateTime.now.minusYears(1)

	private def getPoint(name: String): MonitoringPoint = {
		val point = new MonitoringPoint
		point.pointSet = set
		point.name = name
		point.createdDate = DateTime.now.minusYears(1)
		point.updatedDate = DateTime.now.minusYears(1)
		point.validFromWeek = 1
		point.requiredFromWeek = 1
		point.relationshipService = mockRelationshipService
		point.moduleAndDepartmentService = mockModuleAndDepartmentService
		point.assignmentService = mockAssignmentService
		point
	}

	val standardPoint = getPoint("standard")
	standardPoint.pointType = null
	set.points.add(standardPoint)

	val meetingPoint = getPoint("meeting")
	meetingPoint.pointType = MonitoringPointType.Meeting
	meetingPoint.meetingRelationships = Seq(tutorRelationshipType)
	meetingPoint.meetingFormats = Seq(MeetingFormat.FaceToFace)
	meetingPoint.meetingQuantity = 2
	set.points.add(meetingPoint)

	val groupPoint = getPoint("group")
	groupPoint.pointType = MonitoringPointType.SmallGroup
	groupPoint.smallGroupEventQuantity = 2
	groupPoint.smallGroupEventModules = Seq(module)
	set.points.add(groupPoint)

	val assignmentPoint = getPoint("assignment")
	assignmentPoint.pointType = MonitoringPointType.AssignmentSubmission
	assignmentPoint.assignmentSubmissionIsSpecificAssignments = true
	assignmentPoint.assignmentSubmissionQuantity = 1
	assignmentPoint.assignmentSubmissionAssignments = Seq(assignment)
	assignmentPoint.assignmentSubmissionIsDisjunction = true
	set.points.add(assignmentPoint)

	val standardPointStudent1Checkpoint = new MonitoringCheckpoint
	standardPointStudent1Checkpoint.monitoringPointService = mockMonitoringPointService
	standardPointStudent1Checkpoint.point = standardPoint
	standardPointStudent1Checkpoint.student = student1
	standardPointStudent1Checkpoint.state = AttendanceState.Attended
	standardPointStudent1Checkpoint.updatedDate = DateTime.now.minusYears(1)
	standardPointStudent1Checkpoint.updatedBy = "cusfal"
	standardPointStudent1Checkpoint.autoCreated = false
	standardPoint.checkpoints.add(standardPointStudent1Checkpoint)

	val standardPointStudent2Checkpoint = new MonitoringCheckpoint
	standardPointStudent2Checkpoint.monitoringPointService = mockMonitoringPointService
	standardPointStudent2Checkpoint.point = standardPoint
	standardPointStudent2Checkpoint.student = student2
	standardPointStudent2Checkpoint.state = AttendanceState.MissedAuthorised
	standardPointStudent2Checkpoint.updatedDate = DateTime.now.minusYears(1)
	standardPointStudent2Checkpoint.updatedBy = "cusfal"
	standardPointStudent2Checkpoint.autoCreated = false
	standardPoint.checkpoints.add(standardPointStudent2Checkpoint)

	val meetingPointStudent1Checkpoint = new MonitoringCheckpoint
	meetingPointStudent1Checkpoint.monitoringPointService = mockMonitoringPointService
	meetingPointStudent1Checkpoint.point = meetingPoint
	meetingPointStudent1Checkpoint.student = student1
	meetingPointStudent1Checkpoint.state = AttendanceState.MissedUnauthorised
	meetingPointStudent1Checkpoint.updatedDate = DateTime.now.minusYears(1)
	meetingPointStudent1Checkpoint.updatedBy = "cusfal"
	meetingPointStudent1Checkpoint.autoCreated = true
	meetingPoint.checkpoints.add(meetingPointStudent1Checkpoint)

	val standardPointStudent1Note = new MonitoringPointAttendanceNote
	standardPointStudent1Note.point = standardPoint
	standardPointStudent1Note.student = student1
	standardPointStudent1Note.updatedDate = DateTime.now.minusYears(1)
	standardPointStudent1Note.updatedBy = "cusfal"
	standardPointStudent1Note.note = "some note"
	standardPointStudent1Note.attachment = new FileAttachment
	standardPointStudent1Note.attachment.id = "1234"
	standardPointStudent1Note.absenceType = AbsenceType.Academic
	mockMonitoringPointService.findAttendanceNotes(Seq(standardPoint, meetingPoint, groupPoint, assignmentPoint)) returns Seq(standardPointStudent1Note)

	trait Fixture {
		val cmd = new MonitoringPointMigrationCommandInternal(set) with TermServiceComponent
			with MonitoringPointServiceComponent with AttendanceMonitoringServiceComponent {
			override val termService = mockTermService
			override val monitoringPointService = mockMonitoringPointService
			override val attendanceMonitoringService = mockAttendanceMonitoringService
		}
	}

	private def checkNewPoint(scheme: AttendanceMonitoringScheme, newPoint: AttendanceMonitoringPoint, oldPoint: MonitoringPoint): Unit = {
		newPoint.scheme should be (scheme)
		newPoint.name should be (oldPoint.name)
		newPoint.createdDate should be (oldPoint.createdDate)
		newPoint.updatedDate should be (oldPoint.updatedDate)
		newPoint.startWeek should be (oldPoint.validFromWeek)
		newPoint.endWeek should be (oldPoint.requiredFromWeek)
		Option(oldPoint.pointType) match {
			case Some(pointType) => newPoint.pointType.dbValue should be (pointType.dbValue)
			case _ => newPoint.pointType should be (AttendanceMonitoringPointType.Standard)
		}

		newPoint.relationshipService = mockRelationshipService
		newPoint.moduleAndDepartmentService = mockModuleAndDepartmentService
		newPoint.assignmentService = mockAssignmentService
	}

	private def checkNewCheckpoint(newCheckpoint: AttendanceMonitoringCheckpoint, oldCheckpoint: MonitoringCheckpoint): Unit = {
		newCheckpoint.student should be (oldCheckpoint.student)
		newCheckpoint.state should be (oldCheckpoint.state)
		newCheckpoint.updatedDate should be (oldCheckpoint.updatedDate)
		newCheckpoint.updatedBy should be (oldCheckpoint.updatedBy)
		newCheckpoint.autoCreated should be (oldCheckpoint.autoCreated)
	}

	val now = DateTime.now

	@Test
	def success(): Unit = new Fixture { withFakeTime(now) {
		// FIXME: Must be a better way of doing this?
		val checkpoints: mutable.Buffer[AttendanceMonitoringCheckpoint] = mutable.Buffer()
		mockAttendanceMonitoringService.saveOrUpdateDangerously(any[AttendanceMonitoringCheckpoint]) answers {any => any match {
			case checkpoint: AttendanceMonitoringCheckpoint => checkpoints += checkpoint
			case _ =>
		}}

		val attendanceNotes: mutable.Buffer[AttendanceMonitoringNote] = mutable.Buffer()
		mockAttendanceMonitoringService.saveOrUpdate(any[AttendanceMonitoringNote]) answers {any => any match {
			case note: AttendanceMonitoringNote => attendanceNotes += note
			case _ =>
		}}

		val scheme = cmd.applyInternal()
		set.migratedTo should be (scheme)
		scheme.department should be (thisRoute.adminDepartment)
		scheme.academicYear should be (thisAcademicYear)
		scheme.createdDate should be (set.createdDate)
		scheme.updatedDate should be (set.updatedDate)
		scheme.pointStyle should be (AttendanceMonitoringPointStyle.Week)
		scheme.members.size should be (2)
		scheme.members.includedUserIds.contains(student1.universityId) should be {true}
		scheme.members.includedUserIds.contains(student2.universityId) should be {true}
		verify(mockAttendanceMonitoringService, times(1)).saveOrUpdate(scheme)

		scheme.points.size should be (4)
		val newStandardPoint = scheme.points.asScala.find(_.name == standardPoint.name).get
		val newMeetingPoint = scheme.points.asScala.find(_.name == meetingPoint.name).get
		val newGroupPoint = scheme.points.asScala.find(_.name == groupPoint.name).get
		val newAssignmentPoint = scheme.points.asScala.find(_.name == assignmentPoint.name).get

		checkNewPoint(scheme, newStandardPoint, standardPoint)
		verify(mockAttendanceMonitoringService, times(1)).saveOrUpdate(newStandardPoint)

		checkNewPoint(scheme, newMeetingPoint, meetingPoint)
		newMeetingPoint.meetingRelationships.forall(meetingPoint.meetingRelationships.contains) should be {true}
		newMeetingPoint.meetingRelationships.size should be (meetingPoint.meetingRelationships.size)
		newMeetingPoint.meetingFormats.forall(meetingPoint.meetingFormats.contains) should be {true}
		newMeetingPoint.meetingFormats.size should be (meetingPoint.meetingFormats.size)
		newMeetingPoint.meetingQuantity should be (meetingPoint.meetingQuantity)
		verify(mockAttendanceMonitoringService, times(1)).saveOrUpdate(newMeetingPoint)

		checkNewPoint(scheme, newGroupPoint, groupPoint)
		newGroupPoint.smallGroupEventQuantity should be (groupPoint.smallGroupEventQuantity)
		newGroupPoint.smallGroupEventModules.forall(groupPoint.smallGroupEventModules.contains) should be {true}
		newGroupPoint.smallGroupEventModules.size should be (groupPoint.smallGroupEventModules.size)
		verify(mockAttendanceMonitoringService, times(1)).saveOrUpdate(newGroupPoint)

		checkNewPoint(scheme, newAssignmentPoint, assignmentPoint)
		newAssignmentPoint.assignmentSubmissionType should be (Settings.AssignmentSubmissionTypes.Assignments)
		newAssignmentPoint.assignmentSubmissionAssignments.forall(assignmentPoint.assignmentSubmissionAssignments.contains) should be {true}
		newAssignmentPoint.assignmentSubmissionAssignments.size should be (assignmentPoint.assignmentSubmissionAssignments.size)
		verify(mockAttendanceMonitoringService, times(1)).saveOrUpdate(newAssignmentPoint)

		checkpoints.size should be (8)
		checkpoints.groupBy(_.point).foreach { case (_, pointCheckpoints) => pointCheckpoints.size should be (2) }

		val newStandardPointStudent1Checkpoint = checkpoints.find(c => c.point == newStandardPoint && c.student == student1).get
		newStandardPointStudent1Checkpoint.point should be (newStandardPoint)
		checkNewCheckpoint(newStandardPointStudent1Checkpoint, standardPointStudent1Checkpoint)

		val newStandardPointStudent2Checkpoint = checkpoints.find(c => c.point == newStandardPoint && c.student == student2).get
		newStandardPointStudent2Checkpoint.point should be (newStandardPoint)
		checkNewCheckpoint(newStandardPointStudent2Checkpoint, standardPointStudent2Checkpoint)

		val newMeetingPointStudent1Checkpoint = checkpoints.find(c => c.point == newMeetingPoint && c.student == student1).get
		newMeetingPointStudent1Checkpoint.point should be (newMeetingPoint)
		checkNewCheckpoint(newMeetingPointStudent1Checkpoint, meetingPointStudent1Checkpoint)

		val otherCheckpoints = checkpoints.diff(Seq(newStandardPointStudent1Checkpoint, newStandardPointStudent2Checkpoint, newMeetingPointStudent1Checkpoint))
		otherCheckpoints.foreach(checkpoint => {
			checkpoint.state should be (AttendanceState.MissedAuthorised)
			checkpoint.updatedDate should be (now)
			checkpoint.updatedBy should be ("tabula")
			checkpoint.autoCreated should be {true}
		})

		attendanceNotes.size should be (1)
		val newStandardPointStudent1Note = attendanceNotes.head
		newStandardPointStudent1Note.point should be (newStandardPoint)
		newStandardPointStudent1Note.student should be (student1)
		newStandardPointStudent1Note.updatedDate should be (standardPointStudent1Note.updatedDate)
		newStandardPointStudent1Note.updatedBy should be (standardPointStudent1Note.updatedBy)
		newStandardPointStudent1Note.note should be (standardPointStudent1Note.note)
		newStandardPointStudent1Note.attachment should be (standardPointStudent1Note.attachment)
		newStandardPointStudent1Note.absenceType should be (standardPointStudent1Note.absenceType)

		verify(mockAttendanceMonitoringService, times(1)).updateCheckpointTotal(student1, thisDepartment, thisAcademicYear)
		verify(mockAttendanceMonitoringService, times(1)).updateCheckpointTotal(student2, thisDepartment, thisAcademicYear)

	}}

}
