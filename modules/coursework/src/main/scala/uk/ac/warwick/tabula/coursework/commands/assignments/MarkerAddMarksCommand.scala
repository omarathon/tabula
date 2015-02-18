package uk.ac.warwick.tabula.coursework.commands.assignments

import org.joda.time.DateTime
import org.springframework.util.StringUtils
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.coursework.services.docconversion.{AutowiringMarksExtractorComponent, MarkItem}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Module, _}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, AutowiringUserLookupComponent, FeedbackServiceComponent, GeneratesGradesFromMarks, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

object MarkerAddMarksCommand {
	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser, firstMarker: Boolean, gradeGenerator: GeneratesGradesFromMarks) =
		new MarkerAddMarksCommandInternal(module, assignment, marker, submitter, firstMarker, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringMarksExtractorComponent
			with ComposableCommand[List[MarkerFeedback]]
			with MarkerAddMarksDescription
			with MarkerAddMarksPermissions
			with MarkerAddMarksCommandValidation
			with MarkerAddMarksCommandState
			with PostExtractValidation
			with AddMarksCommandBindListener
}

class MarkerAddMarksCommandInternal(val module: Module, val assignment: Assignment, val marker: User, val submitter: CurrentUser, val firstMarker: Boolean, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[List[MarkerFeedback]] {

	self: MarkerAddMarksCommandState with FeedbackServiceComponent =>

	override def applyInternal(): List[MarkerFeedback] = transactional() {

		def saveFeedback(universityId: String, actualMark: String, actualGrade: String) = {

			val parentFeedback = assignment.feedbacks.find(_.universityId == universityId).getOrElse({
				val newFeedback = new AssignmentFeedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = marker.getUserId
				newFeedback.universityId = universityId
				newFeedback.released = false
				newFeedback.createdDate = DateTime.now
				newFeedback
			})

			// get marker feedback if it already exists - if not one is automatically created
			val markerFeedback:MarkerFeedback = firstMarker match {
				case true => parentFeedback.retrieveFirstMarkerFeedback
				case false => parentFeedback.retrieveSecondMarkerFeedback
			}

			markerFeedback.mark = StringUtils.hasText(actualMark) match {
				case true => Some(actualMark.toInt)
				case false => None
			}

			markerFeedback.grade = Option(actualGrade)

			parentFeedback.updatedDate = DateTime.now

			feedbackService.saveOrUpdate(parentFeedback)
			feedbackService.save(markerFeedback)
			markerFeedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map { (mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade) }
		markList.toList
	}

}

trait MarkerAddMarksCommandValidation extends ValidatesMarkItem {

	self: MarkerAddMarksCommandState with UserLookupComponent =>

	override def checkMarkUpdated(mark: MarkItem) {
		// Warn if marks for this student are already uploaded
		assignment.feedbacks.find { (feedback) => feedback.universityId == mark.universityId} match {
			case Some(feedback) =>
				if (assignment.isFirstMarker(marker)
					&& feedback.firstMarkerFeedback != null
					&& (feedback.firstMarkerFeedback.hasMark || feedback.firstMarkerFeedback.hasGrade)
				)
					mark.isModified = true
				else if (assignment.isSecondMarker(marker)
					&& feedback.secondMarkerFeedback != null
					&& (feedback.secondMarkerFeedback.hasMark || feedback.secondMarkerFeedback.hasGrade)
				)
					mark.isModified = true
			case None =>
		}
	}
}

trait MarkerAddMarksDescription extends Describable[List[MarkerFeedback]] {

	self: MarkerAddMarksCommandState =>

	override lazy val eventName = "MarkerAddMarks"

	override def describe(d: Description) {
		d.assignment(assignment)
	}
}

trait MarkerAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: MarkerAddMarksCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Marks.Create, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}

}

trait MarkerAddMarksCommandState extends AddMarksCommandState {
	def marker: User
	def submitter: CurrentUser
}
