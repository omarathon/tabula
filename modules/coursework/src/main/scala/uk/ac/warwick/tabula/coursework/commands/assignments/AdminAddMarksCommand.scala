package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Feedback, Assignment}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.CurrentUser

class AdminAddMarksCommand(assignment: Assignment, submitter: CurrentUser)
	extends AddMarksCommand[List[Feedback]](assignment, submitter){

	override def applyInternal(): List[Feedback] = transactional() {
		def saveFeedback(universityId: String, actualMark: String, actualGrade: String) = {
			val feedback = assignment.findFeedback(universityId).getOrElse(new Feedback)
			feedback.assignment = assignment
			feedback.uploaderId = submitter.apparentId
			feedback.universityId = universityId
			feedback.released = false
			feedback.actualMark = Option(actualMark.toInt)
			feedback.actualGrade = actualGrade
			session.saveOrUpdate(feedback)
			feedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map { (mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade) }
		markList.toList
	}

}
