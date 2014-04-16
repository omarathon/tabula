package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.data.model.{Submission, Assignment}
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, CommandInternal}
import uk.ac.warwick.tabula.coursework.web.controllers.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.services.{AssignmentMembershipServiceComponent, AssignmentServiceComponent}
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

trait CourseworkCommandTypes {
	type AssignmentInfo = Map[String, Any]
}

object StudentCourseworkCommand extends CourseworkCommandTypes {

	case class StudentAssignments(
		val enrolledAssignments: Seq[AssignmentInfo],
		val historicAssignments: Seq[AssignmentInfo]
	)
}

trait StudentCourseworkCommandInternal
	extends CommandInternal[StudentAssignments] {
	self: AssignmentServiceComponent with
		AssignmentMembershipServiceComponent with
		FeaturesComponent with
		StudentCourseworkCommandHelper =>

	def applyInternal = StudentAssignments(
		enrolledAssignments = enrolledAssignmentsInfo,
		historicAssignments = getHistoricAssignmentsInfo(assignmentsWithFeedbackInfo, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
	)
}

trait StudentCourseworkCommandHelper
	extends CourseworkCommandTypes
	with TaskBenchmarking {

	self: AssignmentServiceComponent with
		AssignmentMembershipServiceComponent with
		FeaturesComponent =>

	val overridableAssignmentsWithFeedback: Seq[Assignment]
	val overridableEnrolledAssignments: Seq[Assignment]
	val overridableAssignmentsWithSubmission: Seq[Assignment]
	val user: User
	val universityId: String

	val assignmentsWithFeedback = benchmarkTask("Get assignments with feedback") { overridableAssignmentsWithFeedback }

	val enrolledAssignments = benchmarkTask("Get enrolled assignments") {
		if (features.assignmentMembership) overridableEnrolledAssignments
		else Nil
	}

	val assignmentsWithSubmission = benchmarkTask("Get assignments with submission") {
		if (features.submissions) overridableAssignmentsWithSubmission
		else Nil
	}

	val lateFormativeAssignments = {
		enrolledAssignments.filter {
			ass => !ass.summative && ass.isClosed
		}
	} // TAB-706

	// exclude assignments already included in other lists.
	def enrolledAssignmentsTrimmed(user: User, universityId: String) =
		enrolledAssignments
			.diff(assignmentsWithFeedback)
			.diff(assignmentsWithSubmission)
			.filter {_.collectSubmissions} // TAB-475
			.filterNot(lateFormativeAssignments.contains(_))
			.sortWith { (ass1, ass2) =>
				// TAB-569 personal time to deadline - if ass1 is "due" before ass2 for the current user
				// Show open ended assignments after
				if (ass2.openEnded && !ass1.openEnded) true
				else if (ass1.openEnded && !ass2.openEnded) false
				else {
					def timeToDeadline(ass: Assignment) = {
						val extension = ass.extensions.asScala.find(e => e.universityId == universityId || e.userId == user.getUserId)
						val isExtended = ass.isWithinExtension(user)

						if (ass.openEnded) ass.openDate
						else if (isExtended) (extension map { _.expiryDate }).get
						else ass.closeDate
					}

					timeToDeadline(ass1) < timeToDeadline(ass2)
				}
			}

	private def enhanced(assignment: Assignment, user: User, universityId: String) = {
		val extension = assignment.extensions.asScala.find(e => e.isForUser(user))
		val isExtended = assignment.isWithinExtension(user)
		val extensionRequested = extension.isDefined && !extension.get.isManual
		val submission = assignment.submissions.asScala.find(_.universityId == universityId)
		val feedback = assignment.feedbacks.asScala.filter(_.released).find(_.universityId == universityId)
		Map(
			"assignment" -> assignment,
			"submission" -> submission,
			"hasSubmission" -> submission.isDefined,
			"feedback" -> feedback,
			"hasFeedback" -> feedback.isDefined,
			"hasExtension" -> extension.isDefined,
			"extension" -> extension,
			"isExtended" -> isExtended,
			"extensionRequested" -> extensionRequested,
			"submittable" -> assignment.submittable(user),
			"resubmittable" -> assignment.resubmittable(user),
			"closed" -> assignment.isClosed,
			"summative" -> assignment.summative.booleanValue
		)

	}

	// adorn the enrolled assignments with extra data.
	val enrolledAssignmentsInfo = for (assignment <- enrolledAssignmentsTrimmed(user, universityId)) yield enhanced(assignment, user, universityId)
	val assignmentsWithFeedbackInfo = for (assignment <- assignmentsWithFeedback) yield enhanced(assignment, user, universityId)
	val assignmentsWithSubmissionInfo = for (assignment <- assignmentsWithSubmission.diff(assignmentsWithFeedback)) yield enhanced(assignment, user, universityId)
	val lateFormativeAssignmentsInfo = for (assignment <- lateFormativeAssignments) yield enhanced(assignment, user, universityId)

	def getHistoricAssignmentsInfo(
																	assignmentsWithFeedbackInfo: Seq[AssignmentInfo],
																	assignmentsWithSubmissionInfo: Seq[AssignmentInfo],
																	lateFormativeAssignmentsInfo: Seq[AssignmentInfo]): Seq[AssignmentInfo] = {
		assignmentsWithFeedbackInfo
			.union(assignmentsWithSubmissionInfo)
			.union(lateFormativeAssignmentsInfo)
			.sortWith {	(info1, info2) =>
			def toDate(info: AssignmentInfo) = {
				val assignment = info("assignment").asInstanceOf[Assignment]
				val submission = info("submission").asInstanceOf[Option[Submission]]

				submission map { _.submittedDate } getOrElse { if (assignment.openEnded) assignment.openDate else assignment.closeDate }
			}

			toDate(info1) < toDate(info2)
		}.distinct.reverse
	}
}
