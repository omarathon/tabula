package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Module, MarkerFeedback, Feedback, Assignment}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.actions.UploadMarkerFeedback

class MarkerAddMarksCommand(module: Module, assignment: Assignment, submitter: CurrentUser, val firstMarker:Boolean)
	extends AddMarksCommand[List[MarkerFeedback]](module, assignment, submitter){

	mustBeLinked(assignment, module)
	PermissionsCheck(UploadMarkerFeedback(assignment))

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

			//TODO - UPDATE STATE
			markerFeedback.mark = Option(actualMark.toInt)
			session.saveOrUpdate(parentFeedback)
			session.saveOrUpdate(markerFeedback)
			markerFeedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map { (mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade) }
		markList.toList
	}

}