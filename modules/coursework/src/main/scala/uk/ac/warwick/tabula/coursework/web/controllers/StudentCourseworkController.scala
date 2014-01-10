package uk.ac.warwick.tabula.coursework.web.controllers

import scala.collection.JavaConverters._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.coursework.web.controllers.StudentCourseworkCommand._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AssignmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/student/{member}"))
class StudentCourseworkController extends CourseworkController {
	
	@ModelAttribute("command") def command(@PathVariable member: Member) =
		StudentCourseworkCommand(MemberOrUser(member))

	@RequestMapping
	def listAssignments(@ModelAttribute("command") command: Appliable[StudentAssignments], @PathVariable member: Member, user: CurrentUser): Mav = {
		val info = command.apply()

		Mav("home/_student",
			"student" -> MemberOrUser(member),
			"enrolledAssignments" -> info.enrolledAssignments,
			"historicAssignments" -> info.historicAssignments,
			"isSelf" -> (member.universityId == user.universityId)
		).noLayoutIf(ajax)
	}

}

object StudentCourseworkCommand {
	type AssignmentInfo = Map[String, Any] 
	
	case class StudentAssignments(
		val enrolledAssignments: Seq[AssignmentInfo],
		val historicAssignments: Seq[AssignmentInfo]
	)
	
	def apply(user: MemberOrUser): Appliable[StudentAssignments] =
		new StudentCourseworkCommandInternal(user)
			with ComposableCommand[StudentAssignments]
			with StudentCourseworkCommandPermissions
			with AutowiringAssignmentServiceComponent
			with AutowiringAssignmentMembershipServiceComponent
			with AutowiringFeaturesComponent
			with ReadOnly with Unaudited
	
	def getHistoricAssignmentsInfo(assignmentsWithFeedbackInfo: Seq[AssignmentInfo], assignmentsWithSubmissionInfo: Seq[AssignmentInfo], lateFormativeAssignmentsInfo: Seq[AssignmentInfo]): Seq[AssignmentInfo] = {
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

class StudentCourseworkCommandInternal(val user: MemberOrUser) extends CommandInternal[StudentAssignments] with StudentCourseworkCommandState with TaskBenchmarking {
	self: AssignmentServiceComponent with 
		  AssignmentMembershipServiceComponent with
		  FeaturesComponent =>
	
	def applyInternal() = {
		val assignmentsWithFeedback = benchmarkTask("Get assignments with feedback") { assignmentService.getAssignmentsWithFeedback(user.universityId) }

		val enrolledAssignments = benchmarkTask("Get enrolled assignments") {
			if (features.assignmentMembership) assignmentMembershipService.getEnrolledAssignments(user.asUser)
			else Seq.empty
		}
		
		val assignmentsWithSubmission = benchmarkTask("Get assignments with submission") {
			if (features.submissions) assignmentService.getAssignmentsWithSubmission(user.universityId)
			else Seq.empty
		}
		
		val lateFormativeAssignments = enrolledAssignments.filter { ass => !ass.summative && ass.isClosed } // TAB-706

		// exclude assignments already included in other lists.
		val enrolledAssignmentsTrimmed =
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
							val extension = ass.extensions.asScala.find(e => e.universityId == user.universityId || e.userId == user.usercode)
							val isExtended = ass.isWithinExtension(user.asUser)

							if (ass.openEnded) ass.openDate
							else if (isExtended) (extension map { _.expiryDate }).get
							else ass.closeDate
						}

						timeToDeadline(ass1) < timeToDeadline(ass2)
					}
				}
		
		def enhanced(assignment: Assignment) = {
			val extension = assignment.extensions.asScala.find(e => e.isForUser(user.asUser))
			val isExtended = assignment.isWithinExtension(user.asUser)
			val extensionRequested = extension.isDefined && !extension.get.isManual
			val submission = assignment.submissions.asScala.find(_.universityId == user.universityId)
			val feedback = assignment.feedbacks.asScala.filter(_.released).find(_.universityId == user.universityId)
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
				"submittable" -> assignment.submittable(user.asUser),
				"resubmittable" -> assignment.resubmittable(user.asUser),
				"closed" -> assignment.isClosed,
				"summative" -> assignment.summative.booleanValue
			)
		}
			
		// adorn the enrolled assignments with extra data.
		val enrolledAssignmentsInfo = for (assignment <- enrolledAssignmentsTrimmed) yield enhanced(assignment)
		val assignmentsWithFeedbackInfo = for (assignment <- assignmentsWithFeedback) yield enhanced(assignment)
		val assignmentsWithSubmissionInfo = for (assignment <- assignmentsWithSubmission.diff(assignmentsWithFeedback)) yield enhanced(assignment)
		val lateFormativeAssignmentsInfo = for (assignment <- lateFormativeAssignments) yield enhanced(assignment)
		
		StudentAssignments(
			enrolledAssignments = enrolledAssignmentsInfo,
			historicAssignments = getHistoricAssignmentsInfo(assignmentsWithFeedbackInfo, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
		)
	}
	
}

trait StudentCourseworkCommandState {
	def user: MemberOrUser
}

trait StudentCourseworkCommandPermissions extends RequiresPermissionsChecking {
	self: StudentCourseworkCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		user.asMember.foreach { member =>
			p.PermissionCheck(Permissions.Profiles.Read.Coursework, member)
			p.PermissionCheck(Permissions.Submission.Read, member)
			p.PermissionCheck(Permissions.Feedback.Read, member)
			p.PermissionCheck(Permissions.Extension.Read, member)
		}
	}
}