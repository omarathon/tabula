package uk.ac.warwick.tabula.coursework.commands.assignments

import org.joda.time.DateTime
import org.springframework.util.StringUtils
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackChangeNotification
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.GeneratesGradesFromMarks

import scala.collection.JavaConversions._

class AdminAddMarksCommand(module:Module, assignment: Assignment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks)
	extends AddMarksCommand[Seq[Feedback]](module, assignment, submitter.apparentUser, gradeGenerator) with Notifies[Seq[Feedback], Feedback] {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Marks.Create, assignment)
	
	var updatedReleasedFeedback: Seq[Feedback] = Nil

	override def checkMarkUpdated(mark: MarkItem) {
		// Warn if marks for this student are already uploaded
		assignment.feedbacks.find { (feedback) => feedback.universityId == mark.universityId && (feedback.hasMark || feedback.hasGrade) } match {
			case Some(feedback) =>
				val markChanged = feedback.actualMark match {
					case Some(m) if m.toString != mark.actualMark => true
					case _ => false
				}
				val gradeChanged = {
					if (module.adminDepartment.assignmentGradeValidation) {
						markChanged
					} else {
						feedback.actualGrade match {
							case Some(g) if g != mark.actualGrade => true
							case _ => false
						}
					}
				}
				if (markChanged || gradeChanged){
					mark.isModified = true
					mark.isPublished = feedback.released
					mark.hasAdjustment = Seq(feedback.adjustedMark, feedback.adjustedGrade).flatten.nonEmpty
				}
			case None =>
		}
	}

	override def applyInternal(): List[Feedback] = transactional() {
		def saveFeedback(universityId: String, actualMark: String, actualGrade: String, isModified: Boolean) = {
			val feedback = assignment.findFeedback(universityId).getOrElse({
				val newFeedback = new AssignmentFeedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = submitter.apparentId
				newFeedback.universityId = universityId
				newFeedback.released = false
				newFeedback.createdDate = DateTime.now
				newFeedback
			})

			feedback.actualMark = StringUtils.hasText(actualMark) match {
				case true => Some(actualMark.toInt)
				case false => None
			}

			feedback.actualGrade = Option(actualGrade)

			feedback.updatedDate = DateTime.now

			session.saveOrUpdate(feedback)

			if (feedback.released && isModified) {
				updatedReleasedFeedback = feedback +: updatedReleasedFeedback
			}

			feedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map {
			(mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade, mark.isModified)
		}

		markList.toList
	}

	def emit(updatedFeedback: Seq[Feedback]) = updatedReleasedFeedback.flatMap {
		case assignmentFeedback: AssignmentFeedback =>
			Option(Notification.init(new FeedbackChangeNotification, submitter.apparentUser, assignmentFeedback, assignment))
		case _ =>
			None
	}

}
