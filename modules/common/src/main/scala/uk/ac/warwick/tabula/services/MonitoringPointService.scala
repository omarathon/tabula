package uk.ac.warwick.tabula.services


import scala.collection.JavaConverters.asScalaBufferConverter

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent, AutowiringMonitoringPointDaoComponent, MonitoringPointDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointType, MonitoringCheckpointState, MonitoringPointSet, MonitoringPointSetTemplate, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import org.joda.time.DateTime
import scala.Some
import uk.ac.warwick.util.termdates.Term

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
	def getPointById(id : String) : Option[MonitoringPoint]
	def getSetById(id : String) : Option[MonitoringPointSet]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]): Option[MonitoringPointSet]
	def getCheckpointsBySCD(monitoringPoints: Seq[MonitoringPoint]) : Seq[(StudentCourseDetails, MonitoringCheckpoint)]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String) : Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
	def getChecked(members: Seq[StudentMember], set: MonitoringPointSet): Map[StudentMember, Map[MonitoringPoint, Option[MonitoringCheckpointState]]]
	def deleteCheckpoint(scjCode: String, point: MonitoringPoint): Unit
	def saveOrUpdateCheckpoint(
		studentCourseDetails: StudentCourseDetails,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		user: CurrentUser
	) : MonitoringCheckpoint
	def saveOrUpdateCheckpoint(
		studentCourseDetails: StudentCourseDetails,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		member: Member
	) : MonitoringCheckpoint
	def countMissedPoints(student: StudentMember, academicYear: AcademicYear): Int
	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]]
}


abstract class AbstractMonitoringPointService extends MonitoringPointService {
	self: MonitoringPointDaoComponent =>

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = monitoringPointDao.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPoint) = monitoringPointDao.delete(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = monitoringPointDao.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet) = monitoringPointDao.saveOrUpdate(set)
	def saveOrUpdate(template: MonitoringPointSetTemplate) = monitoringPointDao.saveOrUpdate(template)
	def getPointById(id: String): Option[MonitoringPoint] = monitoringPointDao.getPointById(id)
	def getSetById(id: String): Option[MonitoringPointSet] = monitoringPointDao.getSetById(id)
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet] = monitoringPointDao.findMonitoringPointSets(route)
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet] =
		monitoringPointDao.findMonitoringPointSets(route, academicYear)
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]) =
		monitoringPointDao.findMonitoringPointSet(route, academicYear, year)
	
	def getCheckpointsBySCD(monitoringPoints: Seq[MonitoringPoint]): Seq[(StudentCourseDetails, MonitoringCheckpoint)] =
		monitoringPointDao.getCheckpointsBySCD(monitoringPoints)

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

	def deleteCheckpoint(scjCode: String, point: MonitoringPoint): Unit = {
		monitoringPointDao.getCheckpoint(point, scjCode) match {
			case None => // already gone
			case Some(checkpoint) => monitoringPointDao.deleteCheckpoint(checkpoint)
		}
	}

	def saveOrUpdateCheckpoint(
		studentCourseDetails: StudentCourseDetails,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		user: CurrentUser
	) : MonitoringCheckpoint = saveOrUpdateCheckpointForUser(studentCourseDetails, point, state, user.apparentId)

	def saveOrUpdateCheckpoint(
		studentCourseDetails: StudentCourseDetails,
		point: MonitoringPoint,
		state: MonitoringCheckpointState,
		member: Member
	) : MonitoringCheckpoint =
		saveOrUpdateCheckpointForUser(studentCourseDetails, point, state, member.userId)

	private def saveOrUpdateCheckpointForUser(studentCourseDetails: StudentCourseDetails,
		point: MonitoringPoint,	state: MonitoringCheckpointState,	usercode: String
	) : MonitoringCheckpoint = {
		val checkpoint = monitoringPointDao.getCheckpoint(point, studentCourseDetails.student).getOrElse({
			val newCheckpoint = new MonitoringCheckpoint
			newCheckpoint.point = point
			newCheckpoint.studentCourseDetail = studentCourseDetails
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
		student.studentCourseDetails.asScala.map{scd =>
			monitoringPointDao.missedCheckpoints(scd, academicYear)
		}.sum
	}

	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet] = {
		monitoringPointDao.findPointSetsForStudents(students, academicYear)
	}

	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]] = {
		monitoringPointDao.findSimilarPointsForMembers(point, students)
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
	self: MonitoringPointDaoComponent with MeetingRecordDaoComponent with RelationshipServiceComponent with TermServiceComponent =>

	def willCheckpointBeCreated(
		student: StudentMember,
		relationshipType: StudentRelationshipType,
		meetingFormat: MeetingFormat,
		meetingDate: DateTime,
		meetingToSkipApproval: Option[MeetingRecord]
	): Boolean = {
		student.studentCourseDetails.asScala.exists(scd => {
			getRelevantPoints(scd, relationshipType, meetingFormat, meetingDate).exists(point => {
				meetingToSkipApproval match {
					case Some(meeting) => (countRelevantMeetings(scd, point, meetingToSkipApproval) >= point.meetingQuantity)
					case None => (countRelevantMeetings(scd, point, None) >= point.meetingQuantity - 1)
				}
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
			val createdCheckpoints = student.studentCourseDetails.asScala.flatMap(scd => {
				val relevantMeetingPoints = getRelevantPoints(scd, meeting.relationship.relationshipType, meeting.format, meeting.meetingDate)
				// check the required quantity and create a checkpoint if there are sufficient meetings
				val checkpointOptions = for (point <- relevantMeetingPoints) yield {
					if (countRelevantMeetings(scd, point, None) >= point.meetingQuantity) {
						val checkpoint = new MonitoringCheckpoint
						checkpoint.point = point
						checkpoint.studentCourseDetail = scd
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
			})
			createdCheckpoints
		}).getOrElse(Seq())
	}

	private def getRelevantPoints(scd: StudentCourseDetails, relationshipType: StudentRelationshipType, format: MeetingFormat, date: DateTime) = {
		scd.studentCourseYearDetails.asScala.flatMap(scyd => {
			val relevantPointSets = monitoringPointDao.findMonitoringPointSets(scd.route, scyd.academicYear).filter(pointSet =>
				pointSet.year == null || pointSet.year == scyd.yearOfStudy
			)
			val relevantPoints = relevantPointSets.flatMap(_.points.asScala).filter(point =>
				// only points relevant to this meeting
				point.pointType == MonitoringPointType.Meeting
					&& point.meetingRelationships.contains(relationshipType)
					&& point.meetingFormats.contains(format)
					// disregard any points that already have a checkpoint
					&& (monitoringPointDao.getCheckpoint(point, scd.scjCode) match {
						case Some(_: MonitoringCheckpoint) => false
						case None => true
					})
					// check date between point weeks
					&& isDateValidForPoint(point, date)
			)
			relevantPoints
		})
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
	private def countRelevantMeetings(scd: StudentCourseDetails, point: MonitoringPoint, meetingToSkipApproval: Option[MeetingRecord]): Int = {
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
	with AutowiringMonitoringPointDaoComponent
	with AutowiringMeetingRecordDaoComponent
	with AutowiringRelationshipServiceComponent
	with AutowiringTermServiceComponent