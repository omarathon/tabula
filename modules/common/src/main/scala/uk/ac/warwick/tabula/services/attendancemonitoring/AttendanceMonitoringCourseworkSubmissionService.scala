package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Assignment, StudentMember, Submission}
import uk.ac.warwick.tabula.services.{AssignmentServiceComponent, AutowiringAssignmentServiceComponent, AutowiringProfileServiceComponent, ProfileServiceComponent}

trait AttendanceMonitoringCourseworkSubmissionServiceComponent {
	def attendanceMonitoringCourseworkSubmissionService: AttendanceMonitoringCourseworkSubmissionService
}

trait AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent extends AttendanceMonitoringCourseworkSubmissionServiceComponent {
	val attendanceMonitoringCourseworkSubmissionService = Wire[AttendanceMonitoringCourseworkSubmissionService]
}

trait AttendanceMonitoringCourseworkSubmissionService {
	def getCheckpoints(submission: Submission): Seq[AttendanceMonitoringCheckpoint]
	def updateCheckpoints(submission: Submission): Seq[AttendanceMonitoringCheckpoint]
}

abstract class AbstractAttendanceMonitoringCourseworkSubmissionService extends AttendanceMonitoringCourseworkSubmissionService {

	self: ProfileServiceComponent with AttendanceMonitoringServiceComponent with AssignmentServiceComponent =>

	def getCheckpoints(submission: Submission): Seq[AttendanceMonitoringCheckpoint] = {
		if (submission.isLate) {
			Seq()
		} else {
			profileService.getMemberByUniversityId(submission.universityId).flatMap{
				case studentMember: StudentMember =>
						val relevantPoints = getRelevantPoints(
							attendanceMonitoringService.listStudentsPoints(studentMember, None, submission.assignment.academicYear),
							submission,
							studentMember
						)
						val checkpoints = relevantPoints.filter(point => checkQuantity(point, submission, studentMember)).map(point => {
							val checkpoint = new AttendanceMonitoringCheckpoint
							checkpoint.autoCreated = true
							checkpoint.point = point
							checkpoint.attendanceMonitoringService = attendanceMonitoringService
							checkpoint.student = studentMember
							checkpoint.updatedBy = submission.userId
							checkpoint.updatedDate = DateTime.now
							checkpoint.state = AttendanceState.Attended
							checkpoint
						})
						Option(checkpoints)
				case _ => None
			}.getOrElse(Seq())
		}
	}

	def updateCheckpoints(submission: Submission): Seq[AttendanceMonitoringCheckpoint] = {
		getCheckpoints(submission).flatMap(checkpoint => {
			attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
		})
	}

	private def getRelevantPoints(points: Seq[AttendanceMonitoringPoint], submission: Submission, studentMember: StudentMember): Seq[AttendanceMonitoringPoint] = {
		points.filter(point =>
			// Is it the correct type
			point.pointType == AttendanceMonitoringPointType.AssignmentSubmission
				// Is the assignment's due date inside the point's weeks
				&& point.isDateValidForPoint(submission.assignment.closeDate.toLocalDate)
				// Is the submission's assignment or module valid
				&& isAssignmentOrModuleValidForPoint(point, submission.assignment)
				// Is there no existing checkpoint
				&& attendanceMonitoringService.getCheckpoints(Seq(point), Seq(studentMember)).isEmpty
				// The student hasn't been sent to SITS for this point
				&& !attendanceMonitoringService.studentAlreadyReportedThisTerm(studentMember, point)
		)
	}

	private def isAssignmentOrModuleValidForPoint(point: AttendanceMonitoringPoint, assignment: Assignment) = {
		if (point.assignmentSubmissionIsSpecificAssignments)
			point.assignmentSubmissionAssignments.contains(assignment)
		else
			point.assignmentSubmissionModules.contains(assignment.module)
	}

	private def checkQuantity(point: AttendanceMonitoringPoint, submission: Submission, studentMember: StudentMember): Boolean = {
		if (point.assignmentSubmissionIsSpecificAssignments) {
			if (point.assignmentSubmissionIsDisjunction) {
				true
			} else {
				val submissions = assignmentService.getSubmissionsForAssignmentsBetweenDates(
					studentMember.universityId,
					point.startDate.toDateTimeAtStartOfDay,
					point.endDate.plusDays(1).toDateTimeAtStartOfDay
				).filterNot(_.isLate).filterNot(s => s.assignment == submission.assignment) ++ Seq(submission)

				point.assignmentSubmissionAssignments.forall(a => submissions.exists(s => s.assignment == a))
			}
		} else {
			if (point.assignmentSubmissionQuantity == 1) {
				true
			} else {
				val submissions = (
					assignmentService.getSubmissionsForAssignmentsBetweenDates(
						studentMember.universityId,
						point.startDate.toDateTimeAtStartOfDay,
						point.endDate.plusDays(1).toDateTimeAtStartOfDay
					).filterNot(_.isLate).filterNot(
						s => s.assignment == submission.assignment
					) ++ Seq(submission)
				).filter(s => point.assignmentSubmissionModules.contains(s.assignment.module))

				submissions.size >= point.assignmentSubmissionQuantity
			}
		}
	}
}

@Service("attendanceMonitoringCourseworkSubmissionService")
class AttendanceMonitoringCourseworkSubmissionServiceImpl
	extends AbstractAttendanceMonitoringCourseworkSubmissionService
	with AutowiringAttendanceMonitoringServiceComponent
	with AutowiringProfileServiceComponent
	with AutowiringAssignmentServiceComponent
