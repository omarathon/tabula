package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent, AutowiringMonitoringPointDaoComponent, MonitoringPointDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import org.joda.time.DateTime
import uk.ac.warwick.util.termdates.Term
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupEventAttendance}
import uk.ac.warwick.tabula.commands.MemberOrUser

trait MonitoringPointServiceComponent {
	def monitoringPointService: MonitoringPointService
}

trait AutowiringMonitoringPointServiceComponent extends MonitoringPointServiceComponent {
	var monitoringPointService = Wire[MonitoringPointService]
}

trait MonitoringPointService {
	def saveOrUpdate(monitoringPoint : MonitoringPoint)
	def delete(monitoringPoint : MonitoringPoint)
	def saveOrUpdate(monitoringPoint : MonitoringPointTemplate)
	def delete(monitoringPoint : MonitoringPointTemplate)
	def saveOrUpdate(set: MonitoringPointSet)
	def saveOrUpdate(template: MonitoringPointSetTemplate)
	def saveOrUpdate(report : MonitoringPointReport)
	def saveOrUpdate(note: MonitoringPointAttendanceNote)
	def getPointById(id : String) : Option[MonitoringPoint]
	def getPointTemplateById(id : String) : Option[MonitoringPointTemplate]
	def getSetById(id : String) : Option[MonitoringPointSet]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]): Option[MonitoringPointSet]
	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint]) : Seq[(StudentMember, MonitoringCheckpoint)]
	def getCheckpoint(student: StudentMember, point: MonitoringPoint) : Option[MonitoringCheckpoint]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String) : Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
	def getCheckpoints(members: Seq[StudentMember], set: MonitoringPointSet): Map[StudentMember, Map[MonitoringPoint, Option[MonitoringCheckpoint]]]
	def deleteCheckpoint(student: StudentMember, point: MonitoringPoint): Unit
	def saveOrUpdateCheckpointByUser(
		student: StudentMember,
		point: MonitoringPoint,
		state: AttendanceState,
		user: CurrentUser,
		autocreated: Boolean = false
	) : MonitoringCheckpoint
	def saveOrUpdateCheckpointByMember(
		student: StudentMember,
		point: MonitoringPoint,
		state: AttendanceState,
		member: Member,
		autocreated: Boolean = false
	) : MonitoringCheckpoint
	def saveOrUpdateCheckpointByUsercode(
		student: StudentMember,
		point: MonitoringPoint,
		state: AttendanceState,
		usercode: String,
		autocreated: Boolean = false
	) : MonitoringCheckpoint
	def getPointSetForStudent(student: StudentMember, academicYear: AcademicYear): Option[MonitoringPointSet]
	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findPointSetsForStudentsByStudent(students: Seq[StudentMember], academicYear: AcademicYear): Map[StudentMember, MonitoringPointSet]
	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]]
	def studentsByMissedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int,
		startWeek: Int = 1,
		endWeek: Int = 52
	): Seq[(StudentMember, Int)]
	def studentsByUnrecordedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		requiredFromWeek: Int = 52,
		startWeek: Int = 1,
		endWeek: Int = 52,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[(StudentMember, Int)]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def findNonReported(students: Seq[StudentMember], academicYear: AcademicYear, period: String): Seq[StudentMember]
	def findUnreportedReports: Seq[MonitoringPointReport]
	def markReportAsPushed(report: MonitoringPointReport): Unit
	def findReports(students: Seq[StudentMember], year: AcademicYear, period: String): Seq[MonitoringPointReport]
	def studentAlreadyReportedThisTerm(student:StudentMember, point:MonitoringPoint): Boolean
	def hasAnyPointSets(department: Department): Boolean
	def getAttendanceNote(student: StudentMember, monitoringPoint: MonitoringPoint): Option[MonitoringPointAttendanceNote]
	def findAttendanceNotes(students: Seq[StudentMember], points: Seq[MonitoringPoint]): Seq[MonitoringPointAttendanceNote]
	def findAttendanceNotes(points: Seq[MonitoringPoint]): Seq[MonitoringPointAttendanceNote]
	def getSetToMigrate: Option[MonitoringPointSet]
}


abstract class AbstractMonitoringPointService extends MonitoringPointService {
	self: MonitoringPointDaoComponent with TermServiceComponent =>

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = monitoringPointDao.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPoint) = monitoringPointDao.delete(monitoringPoint)
	def saveOrUpdate(monitoringPoint: MonitoringPointTemplate) = monitoringPointDao.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPointTemplate) = monitoringPointDao.delete(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = monitoringPointDao.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet) = monitoringPointDao.saveOrUpdate(set)
	def saveOrUpdate(template: MonitoringPointSetTemplate) = monitoringPointDao.saveOrUpdate(template)
	def saveOrUpdate(report : MonitoringPointReport) = monitoringPointDao.saveOrUpdate(report)
	def saveOrUpdate(note: MonitoringPointAttendanceNote) = monitoringPointDao.saveOrUpdate(note)
	def getPointById(id: String): Option[MonitoringPoint] = monitoringPointDao.getPointById(id)
	def getPointTemplateById(id: String): Option[MonitoringPointTemplate] = monitoringPointDao.getPointTemplateById(id)
	def getSetById(id: String): Option[MonitoringPointSet] = monitoringPointDao.getSetById(id)
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet] = monitoringPointDao.findMonitoringPointSets(route)
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet] =
		monitoringPointDao.findMonitoringPointSets(route, academicYear)
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]) =
		monitoringPointDao.findMonitoringPointSet(route, academicYear, year)
	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint]): Seq[(StudentMember, MonitoringCheckpoint)] =
		monitoringPointDao.getCheckpointsByStudent(monitoringPoints)
	def getCheckpoint(student: StudentMember, point: MonitoringPoint) : Option[MonitoringCheckpoint] =
		monitoringPointDao.getCheckpoint(point, student)

	def listTemplates = monitoringPointDao.listTemplates

	def getTemplateById(id: String): Option[MonitoringPointSetTemplate] = monitoringPointDao.getTemplateById(id)

	def deleteTemplate(template: MonitoringPointSetTemplate) = monitoringPointDao.deleteTemplate(template)

	def countCheckpointsForPoint(point: MonitoringPoint) = monitoringPointDao.countCheckpointsForPoint(point)

	def getCheckpoints(members: Seq[StudentMember], set: MonitoringPointSet): Map[StudentMember, Map[MonitoringPoint, Option[MonitoringCheckpoint]]] =
		members.map(member =>	member ->
			set.points.asScala.map(point =>	point ->
				monitoringPointDao.getCheckpoint(point, member)
			).toMap
		).toMap

	def deleteCheckpoint(student: StudentMember, point: MonitoringPoint): Unit = {
		monitoringPointDao.getCheckpoint(point, student) match {
			case None => // already gone
			case Some(checkpoint) => monitoringPointDao.deleteCheckpoint(checkpoint)
		}
	}

	def saveOrUpdateCheckpointByUser(
		student: StudentMember,
		point: MonitoringPoint,
		state: AttendanceState,
		user: CurrentUser,
		autocreated: Boolean = false
	) : MonitoringCheckpoint = saveOrUpdateCheckpointByUsercode(student, point, state, user.apparentId, autocreated)

	def saveOrUpdateCheckpointByMember(
		student: StudentMember,
		point: MonitoringPoint,
		state: AttendanceState,
		member: Member,
		autocreated: Boolean = false
	) : MonitoringCheckpoint =
		saveOrUpdateCheckpointByUsercode(student, point, state, member.userId, autocreated)

	def saveOrUpdateCheckpointByUsercode(
		student: StudentMember,
		point: MonitoringPoint,
		state: AttendanceState,
		usercode: String,
		autocreated: Boolean
	) : MonitoringCheckpoint = {
		val checkpoint = monitoringPointDao.getCheckpoint(point, student).getOrElse({
			val newCheckpoint = new MonitoringCheckpoint
			newCheckpoint.point = point
			newCheckpoint.student = student
			newCheckpoint.updatedBy = usercode
			newCheckpoint.updatedDate = DateTime.now
			newCheckpoint.state = state
			newCheckpoint.autoCreated = autocreated
			monitoringPointDao.saveOrUpdate(newCheckpoint)
			newCheckpoint
		})
		// TAB-2087
		if (checkpoint.state != state) {
			checkpoint.updatedBy = usercode
			checkpoint.updatedDate = DateTime.now
			checkpoint.state = state
			monitoringPointDao.saveOrUpdate(checkpoint)
		}

		checkpoint
	}

	def getPointSetForStudent(student: StudentMember, academicYear: AcademicYear): Option[MonitoringPointSet] = {
		student.freshStudentCourseDetails.flatMap { scd =>
			scd.freshStudentCourseYearDetails.find { scyd =>
				scyd.academicYear == academicYear
			}.flatMap { scyd =>
				findMonitoringPointSet(scd.currentRoute, academicYear, Option(scyd.yearOfStudy)) orElse findMonitoringPointSet(scd.currentRoute, academicYear, None)
			}
		}.lastOption // StudentCourseDetails is sorted by SCJ code, so we're returning the last valid one
	}

	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet] = {
		monitoringPointDao.findPointSetsForStudents(students, academicYear)
	}

	def findPointSetsForStudentsByStudent(students: Seq[StudentMember], academicYear: AcademicYear): Map[StudentMember, MonitoringPointSet] = {
		monitoringPointDao.findPointSetsForStudentsByStudent(students, academicYear)
	}

	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]] = {
		monitoringPointDao.findSimilarPointsForMembers(point, students)
	}

	def studentsByMissedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int,
		startWeek: Int = 1,
		endWeek: Int = 52
	): Seq[(StudentMember, Int)] = {
		monitoringPointDao.studentsByMissedCount(universityIds, academicYear, isAscending, maxResults, startResult, startWeek, endWeek)
	}

	def studentsByUnrecordedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		requiredFromWeek: Int = 52,
		startWeek: Int = 1,
		endWeek: Int = 52,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[(StudentMember, Int)] = {
		monitoringPointDao.studentsByUnrecordedCount(universityIds, academicYear, requiredFromWeek, startWeek, endWeek, isAscending, maxResults, startResult)
	}

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] = {
		monitoringPointDao.findNonReportedTerms(students, academicYear)
	}

	def findNonReported(students: Seq[StudentMember], academicYear: AcademicYear, period: String): Seq[StudentMember] = {
		monitoringPointDao.findNonReported(students, academicYear, period)
	}

	def findUnreportedReports: Seq[MonitoringPointReport] = {
		monitoringPointDao.findUnreportedReports
	}

	def markReportAsPushed(report: MonitoringPointReport): Unit = {
		report.pushedDate = DateTime.now
		monitoringPointDao.saveOrUpdate(report)
	}

	def findReports(students: Seq[StudentMember], year: AcademicYear, period: String): Seq[MonitoringPointReport] = {
		monitoringPointDao.findReports(students, year, period)
	}

	def studentAlreadyReportedThisTerm(student:StudentMember, point:MonitoringPoint): Boolean = {
		val nonReportedTerms = findNonReportedTerms(Seq(student), point.pointSet.academicYear)
		!nonReportedTerms.contains(termService.getTermFromAcademicWeekIncludingVacations(point.validFromWeek, point.pointSet.academicYear).getTermTypeAsString)
	}

	def hasAnyPointSets(department: Department): Boolean = {
		monitoringPointDao.hasAnyPointSets(department: Department)
	}

	def getAttendanceNote(student: StudentMember, monitoringPoint: MonitoringPoint): Option[MonitoringPointAttendanceNote] = {
		monitoringPointDao.getAttendanceNote(student, monitoringPoint)
	}

	def findAttendanceNotes(students: Seq[StudentMember], points: Seq[MonitoringPoint]): Seq[MonitoringPointAttendanceNote] = {
		monitoringPointDao.findAttendanceNotes(students, points)
	}

	def findAttendanceNotes(points: Seq[MonitoringPoint]): Seq[MonitoringPointAttendanceNote] = {
		monitoringPointDao.findAttendanceNotes(points)
	}

	def getSetToMigrate: Option[MonitoringPointSet] = {
		monitoringPointDao.getSetToMigrate
	}

}

@Service("monitoringPointService")
class MonitoringPointServiceImpl
	extends AbstractMonitoringPointService
	with AutowiringMonitoringPointDaoComponent
	with AutowiringTermServiceComponent



//// MonitoringPointMeetingRelationshipTermService ////


trait MonitoringPointMeetingRelationshipTermServiceComponent {
	def monitoringPointMeetingRelationshipTermService: MonitoringPointMeetingRelationshipTermService
}

trait AutowiringMonitoringPointMeetingRelationshipTermServiceComponent extends MonitoringPointMeetingRelationshipTermServiceComponent {
	var monitoringPointMeetingRelationshipTermService = Wire[MonitoringPointMeetingRelationshipTermService]
}

trait MonitoringPointMeetingRelationshipTermService {
	def willCheckpointBeCreated(
		student: StudentMember,
		relationshipType: StudentRelationshipType,
		meetingFormat: MeetingFormat,
		meetingDate: DateTime,
		meetingToSkipApproval: Option[MeetingRecord]
	): Boolean
	def willCheckpointBeCreated(meeting: MeetingRecord): Boolean
	def updateCheckpointsForMeeting(meeting: MeetingRecord): Seq[MonitoringCheckpoint]
}

abstract class AbstractMonitoringPointMeetingRelationshipTermService extends MonitoringPointMeetingRelationshipTermService {
	self: MonitoringPointServiceComponent with MonitoringPointDaoComponent with MeetingRecordDaoComponent
		with RelationshipServiceComponent with TermServiceComponent=>

	def willCheckpointBeCreated(
		student: StudentMember,
		relationshipType: StudentRelationshipType,
		meetingFormat: MeetingFormat,
		meetingDate: DateTime,
		meetingToSkipApproval: Option[MeetingRecord]
	): Boolean = {
		getRelevantPoints(student, relationshipType, meetingFormat, meetingDate).exists(point => {
			!monitoringPointService.studentAlreadyReportedThisTerm(student, point) && (meetingToSkipApproval match {
				case Some(meeting) => countRelevantMeetings(student, point, meetingToSkipApproval) >= point.meetingQuantity
				case None => countRelevantMeetings(student, point, None) >= point.meetingQuantity - 1
			})
		})
	}

	def willCheckpointBeCreated(meeting: MeetingRecord): Boolean = {
		meeting.relationship.studentMember.exists{student => {
			willCheckpointBeCreated(student, meeting.relationship.relationshipType, meeting.format, meeting.meetingDate, Option(meeting))
		}}
	}

	/**
	 * Creates a monitoring checkpoint for the student associated with the given meeting if:
	 * * the meeting's relationship type and format matches a student's monitoring point
	 * * a checkpoint does not already exist
	 * * enough meetings satisfy for the point's meeting quantity
	 * Only approved meetings that occurred between the point's weeks are considered.
	 */
	def updateCheckpointsForMeeting(meeting: MeetingRecord): Seq[MonitoringCheckpoint] = {
		if (!meeting.isAttendanceApproved) {
			// if the meeting isn't approved do nothing
			return Seq()
		}
		meeting.relationship.studentMember.map(student => {
				val relevantMeetingPoints = getRelevantPoints(student, meeting.relationship.relationshipType, meeting.format, meeting.meetingDate)
				// check the required quantity and create a checkpoint if there are sufficient meetings
				for {
					point <- relevantMeetingPoints
					if !monitoringPointService.studentAlreadyReportedThisTerm(student, point)
					if countRelevantMeetings(student, point, None) >= point.meetingQuantity
				} yield {
					val checkpoint = new MonitoringCheckpoint
					checkpoint.point = point
					checkpoint.monitoringPointService = monitoringPointService
					checkpoint.student = student
					checkpoint.state = AttendanceState.Attended
					checkpoint.autoCreated = true
					checkpoint.updatedDate = DateTime.now
					checkpoint.updatedBy = meeting.relationship.agentMember match {
						case Some(agent: uk.ac.warwick.tabula.data.model.Member) => agent.universityId
						case _ => meeting.relationship.agent
					}
					monitoringPointDao.saveOrUpdate(checkpoint)
					checkpoint
				}
			}).getOrElse(Seq())
	}

	private def getRelevantPoints(student: StudentMember, relationshipType: StudentRelationshipType, format: MeetingFormat, date: DateTime) = {
		val academicYear = AcademicYear.findAcademicYearContainingDate(date)
		monitoringPointService.getPointSetForStudent(student, academicYear).map(set =>
			set.points.asScala.filter(point =>
				// only points relevant to this meeting
				point.pointType == MonitoringPointType.Meeting
					&& point.meetingRelationships.contains(relationshipType)
					&& point.meetingFormats.contains(format)
					// disregard any points that already have a checkpoint
					&& (monitoringPointDao.getCheckpoint(point, student) match {
						case Some(_: MonitoringCheckpoint) => false
						case None => true
					})
					// check date between point weeks
					&& isDateValidForPoint(point, date)
			).toSeq
		).getOrElse(Seq())
	}

	private def isDateValidForPoint(point: MonitoringPoint, date: DateTime) = {
		val dateWeek = termService.getAcademicWeekForAcademicYear(date, point.pointSet.academicYear)
		if (dateWeek == Term.WEEK_NUMBER_BEFORE_START)
			true
		else if (dateWeek == Term.WEEK_NUMBER_AFTER_END)
			false
		else
			point.includesWeek(dateWeek)
	}

	/**
	 * Counts the number of approved meetings relevant to the given point for the given student.
	 * If meetingToSkipApproval is provided, that meeting is included regradless of its approved status,
	 * which is used to check if approving that meeting would then create a checkpoint.
	 */
	private def countRelevantMeetings(student: StudentMember, point: MonitoringPoint, meetingToSkipApproval: Option[MeetingRecord]): Int = {
		point.meetingRelationships.map(relationshipType => {
			relationshipService.getRelationships(relationshipType, student)
				.flatMap(meetingRecordDao.list(_).filter(meeting =>
					(meeting.isAttendanceApproved || meetingToSkipApproval.exists(m => m == meeting))
						&& point.meetingFormats.contains(meeting.format)
						&& isDateValidForPoint(point, meeting.meetingDate)
			)).size
		}).sum
	}
}

@Service("monitoringPointMeetingRelationshipTerm")
class MonitoringPointMeetingRelationshipTermServiceImpl
	extends AbstractMonitoringPointMeetingRelationshipTermService
	with AutowiringMonitoringPointServiceComponent
	with AutowiringMonitoringPointDaoComponent
	with AutowiringMeetingRecordDaoComponent
	with AutowiringRelationshipServiceComponent
	with AutowiringTermServiceComponent



//// MonitoringPointGroupService ////


trait MonitoringPointGroupProfileServiceComponent {
	def monitoringPointGroupProfileService: MonitoringPointGroupProfileService
}

trait AutowiringMonitoringPointGroupProfileServiceComponent extends MonitoringPointGroupProfileServiceComponent {
	var monitoringPointGroupProfileService = Wire[MonitoringPointGroupProfileService]
}

trait MonitoringPointGroupProfileService {
	def getCheckpointsForAttendance(attendance: Seq[SmallGroupEventAttendance]): Seq[MonitoringCheckpoint]
	def updateCheckpointsForAttendance(attendance: Seq[SmallGroupEventAttendance]): Seq[MonitoringCheckpoint]
}

abstract class AbstractMonitoringPointGroupProfileService extends MonitoringPointGroupProfileService {

	self: MonitoringPointServiceComponent with ProfileServiceComponent with SmallGroupServiceComponent =>

	def getCheckpointsForAttendance(attendances: Seq[SmallGroupEventAttendance]): Seq[MonitoringCheckpoint] = {
		attendances.filter(_.state == AttendanceState.Attended).flatMap(attendance => {
			profileService.getMemberByUniversityId(attendance.universityId).flatMap{
				case studentMember: StudentMember =>
					monitoringPointService.getPointSetForStudent(studentMember, attendance.occurrence.event.group.groupSet.academicYear).flatMap(pointSet => {
						val relevantPoints = getRelevantPoints(pointSet.points.asScala, attendance, studentMember)
						val checkpoints = relevantPoints.filter(point => checkQuantity(point, attendance, studentMember)).map(point => {
							val checkpoint = new MonitoringCheckpoint
							checkpoint.point = point
							checkpoint.monitoringPointService = monitoringPointService
							checkpoint.student = studentMember
							checkpoint.updatedBy = attendance.updatedBy
							checkpoint.state = AttendanceState.Attended
							checkpoint
						})
						Option(checkpoints)
					})
				case _ => None
			}
		}).flatten
	}

	def updateCheckpointsForAttendance(attendances: Seq[SmallGroupEventAttendance]): Seq[MonitoringCheckpoint] = {
		getCheckpointsForAttendance(attendances).map(checkpoint => {
			monitoringPointService.saveOrUpdateCheckpointByUsercode(checkpoint.student, checkpoint.point, checkpoint.state, checkpoint.updatedBy, autocreated = true)
		})
	}

	private def getRelevantPoints(points: Seq[MonitoringPoint], attendance: SmallGroupEventAttendance, studentMember: StudentMember): Seq[MonitoringPoint] = {
		points.filter(point =>
			// Is it the correct type
			point.pointType == MonitoringPointType.SmallGroup
			// Is the attendance inside the point's weeks
				&& point.includesWeek(attendance.occurrence.week)
			// Is the group's module valid
				&& (point.smallGroupEventModules.isEmpty || point.smallGroupEventModules.contains(attendance.occurrence.event.group.groupSet.module))
			// Is there no existing checkpoint
				&& monitoringPointService.getCheckpoint(studentMember, point).isEmpty
			// The student hasn't been sent to SITS for this point
				&& !monitoringPointService.studentAlreadyReportedThisTerm(studentMember, point)
		)
	}

	private def checkQuantity(point: MonitoringPoint, attendance: SmallGroupEventAttendance, studentMember: StudentMember): Boolean = {
		if (point.smallGroupEventQuantity == 1) {
			true
		}	else if (point.smallGroupEventQuantity > 1) {
			val attendances = smallGroupService
				.findAttendanceForStudentInModulesInWeeks(studentMember, point.validFromWeek, point.requiredFromWeek, point.smallGroupEventModules)
				.filterNot(a => a.occurrence == attendance.occurrence && a.universityId == attendance.universityId)
			point.smallGroupEventQuantity <= attendances.size + 1
		} else {
			val groups = smallGroupService.findSmallGroupsByStudent(MemberOrUser(studentMember).asUser)
			val allOccurrenceWeeks = groups.filter(g => point.smallGroupEventModules.isEmpty || point.smallGroupEventModules.contains(g.groupSet.module))
				.flatMap(group =>
					group.events.flatMap(event =>
						event.weekRanges.flatMap(_.toWeeks)
					)
				)
			val relevantWeeks = allOccurrenceWeeks.filter(point.includesWeek)
			val attendances = smallGroupService
				.findAttendanceForStudentInModulesInWeeks(studentMember, point.validFromWeek, point.requiredFromWeek, point.smallGroupEventModules)
				.filterNot(a => a.occurrence == attendance.occurrence && a.universityId == attendance.universityId)
			relevantWeeks.size <= attendances.size
		}
	}
}

@Service("monitoringPointGroupProfileService")
class MonitoringPointGroupProfileServiceImpl
	extends AbstractMonitoringPointGroupProfileService
	with AutowiringMonitoringPointServiceComponent
	with AutowiringProfileServiceComponent
	with AutowiringSmallGroupServiceComponent



/// MonitoringPointProfileTermAssignmentService ///


trait MonitoringPointProfileTermAssignmentServiceComponent {
	def monitoringPointProfileTermAssignmentService: MonitoringPointProfileTermAssignmentService
}

trait AutowiringMonitoringPointProfileTermAssignmentServiceComponent extends MonitoringPointProfileTermAssignmentServiceComponent {
	var monitoringPointProfileTermAssignmentService = Wire[MonitoringPointProfileTermAssignmentService]
}

trait MonitoringPointProfileTermAssignmentService {
	def getCheckpointsForSubmission(submission: Submission): Seq[MonitoringCheckpoint]
	def updateCheckpointsForSubmission(submission: Submission): Seq[MonitoringCheckpoint]
}

abstract class AbstractMonitoringPointProfileTermAssignmentService extends MonitoringPointProfileTermAssignmentService {

	self: MonitoringPointServiceComponent with ProfileServiceComponent with TermServiceComponent with AssessmentServiceComponent =>

	def getCheckpointsForSubmission(submission: Submission): Seq[MonitoringCheckpoint] = {
		if (submission.isLate) {
			Seq()
		} else {
			profileService.getMemberByUniversityId(submission.universityId).flatMap{
				case studentMember: StudentMember =>
					monitoringPointService.getPointSetForStudent(studentMember, submission.assignment.academicYear).flatMap(pointSet => {
						val relevantPoints = getRelevantPoints(pointSet.points.asScala, submission, studentMember)
						val checkpoints = relevantPoints.filter(point => checkQuantity(point, submission, studentMember)).map(point => {
							val checkpoint = new MonitoringCheckpoint
							checkpoint.autoCreated = true
							checkpoint.point = point
							checkpoint.monitoringPointService = monitoringPointService
							checkpoint.student = studentMember
							checkpoint.updatedBy = submission.userId
							checkpoint.updatedDate = DateTime.now
							checkpoint.state = AttendanceState.Attended
							checkpoint
						})
						Option(checkpoints)
					})
				case _ => None
			}.getOrElse(Seq())
		}
	}

	def updateCheckpointsForSubmission(submission: Submission): Seq[MonitoringCheckpoint] = {
		getCheckpointsForSubmission(submission).map(checkpoint => {
			monitoringPointService.saveOrUpdateCheckpointByUsercode(checkpoint.student, checkpoint.point, checkpoint.state, checkpoint.updatedBy, autocreated = true)
		})
	}

	private def getRelevantPoints(points: Seq[MonitoringPoint], submission: Submission, studentMember: StudentMember): Seq[MonitoringPoint] = {
		points.filter(point =>
		// Is it the correct type
			point.pointType == MonitoringPointType.AssignmentSubmission
				// Is the assignment's due date inside the point's weeks
				&& isDateValidForPoint(point, submission.assignment.closeDate)
				// Is the submission's assignment or module valid
				&& isAssignmentOrModuleValidForPoint(point, submission.assignment)
				// Is there no existing checkpoint
				&& monitoringPointService.getCheckpoint(studentMember, point).isEmpty
				// The student hasn't been sent to SITS for this point
				&& !monitoringPointService.studentAlreadyReportedThisTerm(studentMember, point)
		)
	}

	private def isDateValidForPoint(point: MonitoringPoint, date: DateTime) = {
		val dateWeek = termService.getAcademicWeekForAcademicYear(date, point.pointSet.academicYear)
		if (dateWeek == Term.WEEK_NUMBER_BEFORE_START)
			true
		else if (dateWeek == Term.WEEK_NUMBER_AFTER_END)
			false
		else
			point.includesWeek(dateWeek)
	}

	private def isAssignmentOrModuleValidForPoint(point: MonitoringPoint, assignment: Assignment) = {
		if (point.assignmentSubmissionIsSpecificAssignments)
			point.assignmentSubmissionAssignments.contains(assignment)
		else
			point.assignmentSubmissionModules.contains(assignment.module)
	}

	private def checkQuantity(point: MonitoringPoint, submission: Submission, studentMember: StudentMember): Boolean = {
		val weeksForYear = termService.getAcademicWeeksForYear(point.pointSet.academicYear.dateInTermOne).toMap
		def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek) =
			weeksForYear(weekNumber).getStart.withDayOfWeek(dayOfWeek.jodaDayOfWeek)

		if (point.assignmentSubmissionIsSpecificAssignments) {
			if (point.assignmentSubmissionIsDisjunction) {
				true
			} else {
				val submissions = assessmentService.getSubmissionsForAssignmentsBetweenDates(
					studentMember.universityId,
					weekNumberToDate(point.validFromWeek, DayOfWeek.Monday),
					weekNumberToDate(point.requiredFromWeek + 1, DayOfWeek.Monday)
				).filterNot(_.isLate).filterNot(s => s.assignment == submission.assignment) ++ Seq(submission)

				point.assignmentSubmissionAssignments.forall(a => submissions.exists(s => s.assignment == a))
			}
		} else {
			if (point.assignmentSubmissionQuantity == 1) {
				true
			} else {
				val submissions = (assessmentService.getSubmissionsForAssignmentsBetweenDates(
						studentMember.universityId,
						weekNumberToDate(point.validFromWeek, DayOfWeek.Monday),
						weekNumberToDate(point.requiredFromWeek + 1, DayOfWeek.Monday)
					).filterNot(_.isLate).filterNot(s => s.assignment == submission.assignment) ++ Seq(submission)
				).filter(s => point.assignmentSubmissionModules.contains(s.assignment.module))

				submissions.size >= point.assignmentSubmissionQuantity
			}
		}
	}
}

@Service("monitoringPointProfileTermAssignmentService")
class MonitoringPointProfileTermAssessmentServiceImpl
	extends AbstractMonitoringPointProfileTermAssignmentService
	with AutowiringMonitoringPointServiceComponent
	with AutowiringProfileServiceComponent
	with AutowiringTermServiceComponent
	with AutowiringAssessmentServiceComponent