package uk.ac.warwick.tabula.coursework.commands.assignments

import org.joda.time.DateTime
import org.springframework.util.StringUtils
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.GeneratesGradesFromMarks
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

class MarkerAddMarksCommand(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser, val firstMarker:Boolean, gradeGenerator: GeneratesGradesFromMarks)
	extends AddMarksCommand[List[MarkerFeedback]](module, assignment, marker, gradeGenerator){

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Marks.Create, assignment)
	if(submitter.apparentUser != marker) {
		PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
	}

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

			session.saveOrUpdate(parentFeedback)
			session.saveOrUpdate(markerFeedback)
			markerFeedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map { (mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade) }
		markList.toList
	}

}