package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Feedback, Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{UploadedFile, Description}
import uk.ac.warwick.tabula.data.Transactions._
import reflect.BeanProperty
import uk.ac.warwick.tabula.helpers.LazyLists


class AddMarkerFeedbackCommand(assignment:Assignment, submitter: CurrentUser, val firstMarker:Boolean)
	extends UploadFeedbackCommand[List[MarkerFeedback]](assignment, submitter)  {

	// list to contain feedback files that are not for a student you should be marking
	@BeanProperty var invalidStudents: JList[FeedbackItem] = LazyLists.simpleFactory()

	val submissions = assignment.getMarkersSubmissions(submitter.apparentUser)

	def processStudents() {
		val universityIds = submissions.map(_.getUniversityId)
		invalidStudents = items.filter(item => !universityIds.contains(item.uniNumber))
		items = items.filter(item => universityIds.contains(item.uniNumber))
	}

	private def saveMarkerFeedback(uniNumber: String, file: UploadedFile) = {
		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.find(_.universityId == uniNumber).getOrElse({
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = submitter.apparentId
			newFeedback.universityId = uniNumber
			newFeedback.released = false
			newFeedback
		})

		// see if marker feedback already exists - if not create one
		val markerFeedback:MarkerFeedback = firstMarker match {
			case true => parentFeedback.retrieveFirstMarkerFeedback
			case false => parentFeedback.retrieveSecondMarkerFeedback
			case _ => null
		}

		for (attachment <- file.attached){
			// if an attachment with the same name as this one exists then delete it
			val duplicateAttachment = markerFeedback.attachments.find(_.name == attachment.name)
			duplicateAttachment.foreach(session.delete(_))
			markerFeedback addAttachment attachment
		}

		session.saveOrUpdate(parentFeedback)
		session.saveOrUpdate(markerFeedback)
		//TODO - UPDATE STATE

		markerFeedback
	}

	override def applyInternal(): List[MarkerFeedback] = transactional() {
		if (items != null && !items.isEmpty) {
			val markerFeedbacks = items.map { (item) =>
				val feedback = saveMarkerFeedback(item.uniNumber, item.file)
				feedback
			}
			markerFeedbacks.toList
		} else {
			val markerFeedbacks = saveMarkerFeedback(uniNumber, file)
			List(markerFeedbacks)
		}
	}

	def describe(d: Description){
		d.assignment(assignment)
		 .studentIds(items.map { _.uniNumber })
	}
}
