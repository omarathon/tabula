package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Module, MarkerFeedback, Feedback, Assignment}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.util.StringUtils

class MarkerAddMarksCommand(module: Module, assignment: Assignment, submitter: CurrentUser, val firstMarker:Boolean)
	extends AddMarksCommand[List[MarkerFeedback]](module, assignment, submitter){

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Marks.Create, assignment)

	override def checkIfDuplicate(mark: MarkItem) {
		// Warn if marks for this student are already uploaded
		assignment.feedbacks.find { (feedback) => feedback.universityId == mark.universityId} match {
			case Some(feedback) => {
				if (assignment.isFirstMarker(submitter.apparentUser) && feedback.firstMarkerFeedback != null && (feedback.firstMarkerFeedback.hasMark || feedback.firstMarkerFeedback.hasGrade))
					mark.warningMessage = markWarning
				else if (assignment.isSecondMarker(submitter.apparentUser) && feedback.secondMarkerFeedback != null && (feedback.secondMarkerFeedback.hasMark || feedback.secondMarkerFeedback.hasGrade))
					mark.warningMessage = markWarning
			}
			case None =>
		}
	}

	override def applyInternal(): List[MarkerFeedback] = transactional() {

		def saveFeedback(universityId: String, actualMark: String, actualGrade: String) = {

			val parentFeedback = assignment.feedbacks.find(_.universityId == universityId).getOrElse({
				val newFeedback = new Feedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = submitter.apparentId
				newFeedback.universityId = universityId
				newFeedback.released = false
				newFeedback
			})

			// get marker feedback if it already exists - if not one is automatically created
			val markerFeedback:MarkerFeedback = firstMarker match {
				case true => parentFeedback.retrieveFirstMarkerFeedback
				case false => parentFeedback.retrieveSecondMarkerFeedback
				case _ => null
			}

			markerFeedback.mark = StringUtils.hasText(actualMark) match {
				case true => Some(actualMark.toInt)
				case false => None
			}
			markerFeedback.grade = Option(actualGrade)
			session.saveOrUpdate(parentFeedback)
			session.saveOrUpdate(markerFeedback)
			markerFeedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map { (mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade) }
		markList.toList
	}

}