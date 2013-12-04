package uk.ac.warwick.tabula.services


import scala.collection.JavaConverters.asScalaBufferConverter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent, AutowiringMonitoringPointDaoComponent, MonitoringPointDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import org.joda.time.DateTime
import uk.ac.warwick.util.termdates.Term
import scala.Some

trait MonitoringPointServiceComponent {
	def monitoringPointService: MonitoringPointService
}

trait AutowiringMonitoringPointServiceComponent extends MonitoringPointServiceComponent {
	var monitoringPointService = Wire[MonitoringPointService]
}

trait MonitoringPointService {
	def saveOrUpdate(monitoringPoint : MonitoringPoint)
	def delete(monitoringPoint : MonitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet)
	def saveOrUpdate(template: MonitoringPointSetTemplate)
	def saveOrUpdate(report : MonitoringPointReport)
	def getPointById(id : String) : Option[MonitoringPoint]
	def getSetById(id : String) : Option[MonitoringPointSet]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]): Option[MonitoringPointSet]
	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint]) : Seq[(StudentMember, MonitoringCheckpoint)]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String) : Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
	def getChecked(members: Seq[StudentMember], set: MonitoringPointSet): Map[StudentMember, Map[MonitoringPoint, Option[MonitoringCheckpointState]]]
	def deleteCheckpoint(student: StudentMember, point: MonitoringPoint): Unit
	def saveOrUpdateCheckpoint(
		student: StudentMember,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		user: CurrentUser
	) : MonitoringCheckpoint
	def saveOrUpdateCheckpoint(
		student: StudentMember,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		member: Member
	) : MonitoringCheckpoint
	def countMissedPoints(student: StudentMember, academicYear: AcademicYear): Int
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
		currentAcademicWeek: Int,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[(StudentMember, Int)]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def findNonReported(students: Seq[StudentMember], academicYear: AcademicYear, period: String): Seq[StudentMember]
	def findUnreportedReports: Seq[MonitoringPointReport]
	def markReportAsPushed(report: MonitoringPointReport): Unit
	def findReports(students: Seq[StudentMember], year: AcademicYear, period: String): Seq[MonitoringPointReport]
}


abstract class AbstractMonitoringPointService extends MonitoringPointService {
	self: MonitoringPointDaoComponent =>

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = monitoringPointDao.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPoint) = monitoringPointDao.delete(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = monitoringPointDao.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet) = monitoringPointDao.saveOrUpdate(set)
	def saveOrUpdate(template: MonitoringPointSetTemplate) = monitoringPointDao.saveOrUpdate(template)
	def saveOrUpdate(report : MonitoringPointReport) = monitoringPointDao.saveOrUpdate(report)
	def getPointById(id: String): Option[MonitoringPoint] = monitoringPointDao.getPointById(id)
	def getSetById(id: String): Option[MonitoringPointSet] = monitoringPointDao.getSetById(id)
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet] = monitoringPointDao.findMonitoringPointSets(route)
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet] =
		monitoringPointDao.findMonitoringPointSets(route, academicYear)
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]) =
		monitoringPointDao.findMonitoringPointSet(route, academicYear, year)
	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint]): Seq[(StudentMember, MonitoringCheckpoint)] =
		monitoringPointDao.getCheckpointsByStudent(monitoringPoints)

	def listTemplates = monitoringPointDao.listTemplates

	def getTemplateById(id: String): Option[MonitoringPointSetTemplate] = monitoringPointDao.getTemplateById(id)

	def deleteTemplate(template: MonitoringPointSetTemplate) = monitoringPointDao.deleteTemplate(template)

	def countCheckpointsForPoint(point: MonitoringPoint) = monitoringPointDao.countCheckpointsForPoint(point)

	def getChecked(members: Seq[StudentMember], set: MonitoringPointSet): Map[StudentMember, Map[MonitoringPoint, Option[MonitoringCheckpointState]]] =
		members.map(member =>
			member ->
			set.points.asScala.map(point =>
				(point, monitoringPointDao.getCheckpoint(point, member) match {
					case Some(c: MonitoringCheckpoint) => Option(c.state)
					case None => None
				})
			).toMap
		).toMap

	def deleteCheckpoint(student: StudentMember, point: MonitoringPoint): Unit = {
		monitoringPointDao.getCheckpoint(point, student) match {
			case None => // already gone
			case Some(checkpoint) => monitoringPointDao.deleteCheckpoint(checkpoint)
		}
	}

	def saveOrUpdateCheckpoint(
		student: StudentMember,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		user: CurrentUser
	) : MonitoringCheckpoint = saveOrUpdateCheckpointForUser(student, point, state, user.apparentId)

	def saveOrUpdateCheckpoint(
		student: StudentMember,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		member: Member
	) : MonitoringCheckpoint =
		saveOrUpdateCheckpointForUser(student, point, state, member.userId)

	private def saveOrUpdateCheckpointForUser(student: StudentMember,
		point: MonitoringPoint,	state: MonitoringCheckpointState,	usercode: String
	) : MonitoringCheckpoint = {
		val checkpoint = monitoringPointDao.getCheckpoint(point, student).getOrElse({
			val newCheckpoint = new MonitoringCheckpoint
			newCheckpoint.point = point
			newCheckpoint.studentCourseDetail = student.mostSignificantCourseDetails.getOrElse(throw new IllegalArgumentException)
			newCheckpoint
		})
		checkpoint.state = state
		checkpoint.updatedBy = usercode
		checkpoint.updatedDate = DateTime.now
		checkpoint.autoCreated = false
		monitoringPointDao.saveOrUpdate(checkpoint)
		checkpoint
	}

	def countMissedPoints(student: StudentMember, academicYear: AcademicYear): Int = {
		monitoringPointDao.missedCheckpoints(student, academicYear)
	}

	def getPointSetForStudent(student: StudentMember, academicYear: AcademicYear): Option[MonitoringPointSet] = {
		student.mostSignificantCourseDetails.flatMap{ scd =>
			scd.freshStudentCourseYearDetails.find(scyd =>
				scyd.academicYear == academicYear
			).flatMap{ scyd =>
				findMonitoringPointSet(scd.route, academicYear, Option(scyd.yearOfStudy)) orElse findMonitoringPointSet(scd.route, academicYear, None)
			}
		}
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
		currentAcademicWeek: Int,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[(StudentMember, Int)] = {
		monitoringPointDao.studentsByUnrecordedCount(universityIds, academicYear, currentAcademicWeek, isAscending, maxResults, startResult)
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

}

@Service("monitoringPointService")
class MonitoringPointServiceImpl
	extends AbstractMonitoringPointService
	with AutowiringMonitoringPointDaoComponent





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
			meetingToSkipApproval match {
				case Some(meeting) => countRelevantMeetings(student, point, meetingToSkipApproval) >= point.meetingQuantity
				case None => countRelevantMeetings(student, point, None) >= point.meetingQuantity - 1
			}
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
				val checkpointOptions = for (point <- relevantMeetingPoints) yield {
					if (countRelevantMeetings(student, point, None) >= point.meetingQuantity) {
						val checkpoint = new MonitoringCheckpoint
						checkpoint.point = point
						checkpoint.studentCourseDetail = student.mostSignificantCourseDetails.getOrElse(throw new IllegalArgumentException)
						checkpoint.state = MonitoringCheckpointState.Attended
						checkpoint.autoCreated = true
						checkpoint.updatedDate = DateTime.now
						checkpoint.updatedBy = meeting.relationship.agentMember match {
							case Some(agent: uk.ac.warwick.tabula.data.model.Member) => agent.universityId
							case _ => meeting.relationship.agent
						}
						monitoringPointDao.saveOrUpdate(checkpoint)
						Option(checkpoint)
					}
					else
						None
				}
				checkpointOptions.flatten.toSeq
			}).getOrElse(Seq())
	}

	private def getRelevantPoints(student: StudentMember, relationshipType: StudentRelationshipType, format: MeetingFormat, date: DateTime) = {
		val academicYear = AcademicYear.findAcademicYearContainingDate(date, termService)
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
		val dateWeek = termService.getAcademicWeekForAcademicYear(date, point.pointSet.asInstanceOf[MonitoringPointSet].academicYear)
		if (dateWeek == Term.WEEK_NUMBER_BEFORE_START)
			true
		else if (dateWeek == Term.WEEK_NUMBER_AFTER_END)
			false
		else
			dateWeek >= point.validFromWeek && dateWeek <= point.requiredFromWeek
	}

	/**
	 * Counts the number of approved meetings relevant to the given point for the given student.
	 * If meetingToSkipApproval is provided, that meeting is included regradless of its approved status,
	 * which is used to check if approving that meeting would then create a checkpoint.
	 */
	private def countRelevantMeetings(student: StudentMember, point: MonitoringPoint, meetingToSkipApproval: Option[MeetingRecord]): Int = {
		val scd = student.mostSignificantCourseDetails.getOrElse(throw new IllegalArgumentException)
		point.meetingRelationships.map(relationshipType => {
			relationshipService.getRelationships(relationshipType, scd.sprCode)
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
