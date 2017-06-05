package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import org.springframework.util.StringUtils
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.coursework.docconversion.{AutowiringMarksExtractorComponent, MarkItem}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackChangeNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

object OldAdminAddMarksCommand {
	def apply(module: Module, assessment: Assessment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new OldAdminAddMarksCommandInternal(module, assessment, submitter, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringMarksExtractorComponent
			with AutowiringSubmissionServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[Seq[Feedback]]
			with OldAdminAddMarksDescription
			with OldAdminAddMarksPermissions
			with OldAdminAddMarksCommandValidation
			with OldAdminAddMarksNotifications
			with OldAdminAddMarksCommandState
			with PostExtractValidation
			with AddMarksCommandBindListener
			with FetchDisabilities
}

class OldAdminAddMarksCommandInternal(val module: Module, val assessment: Assessment, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[Seq[Feedback]] {

	self: OldAdminAddMarksCommandState with FeedbackServiceComponent =>

	override def applyInternal(): List[Feedback] = transactional() {
		def saveFeedback(user: User, actualMark: String, actualGrade: String, isModified: Boolean) = {

			val feedback = assessment.findFeedback(user.getUserId).getOrElse({
				val newFeedback = assessment match {
					case assignment: Assignment =>
						val f = new AssignmentFeedback
						f.assignment = assignment
						f
					case exam: Exam =>
						val f = new ExamFeedback
						f.exam = exam
						f
				}
				newFeedback.uploaderId = submitter.apparentId
				newFeedback.usercode = user.getUserId
				newFeedback._universityId = user.getWarwickId
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

			feedbackService.saveOrUpdate(feedback)

			if (feedback.released && isModified) {
				updatedReleasedFeedback = feedback +: updatedReleasedFeedback
			}

			feedback
		}


		val markList = marks.filter(_.isValid).map{ mark =>
			saveFeedback(mark.user, mark.actualMark, mark.actualGrade, mark.isModified)
		}

		markList.toList
	}

}

trait OldAdminAddMarksCommandValidation extends ValidatesMarkItem {

	self: OldAdminAddMarksCommandState with UserLookupComponent =>

	override def checkMarkUpdated(mark: MarkItem) {
		// Warn if marks for this student are already uploaded
		assessment
			.allFeedback
			.find(feedback => feedback.usercode == mark.user.getUserId && (feedback.hasMark || feedback.hasGrade))
			.foreach(feedback => {
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
					mark.hasAdjustment = feedback.hasPrivateOrNonPrivateAdjustments
				}
			})
	}
}

trait OldAdminAddMarksDescription extends Describable[Seq[Feedback]] {

	self: OldAdminAddMarksCommandState =>

	override lazy val eventName = "AdminAddMarks"

	override def describe(d: Description) {
		assessment match {
			case assignment: Assignment => d.assignment(assignment)
			case exam: Exam => d.exam(exam)
		}

	}
}

trait OldAdminAddMarksNotifications extends Notifies[Seq[Feedback], Feedback] {

	self: OldAdminAddMarksCommandState =>

	def emit(updatedFeedback: Seq[Feedback]): Seq[FeedbackChangeNotification] = updatedReleasedFeedback.flatMap { feedback => HibernateHelpers.initialiseAndUnproxy(feedback) match {
		case assignmentFeedback: AssignmentFeedback =>
			Option(Notification.init(new FeedbackChangeNotification, submitter.apparentUser, assignmentFeedback, assignmentFeedback.assignment))
		case _ =>
			None
	}}
}

trait OldAdminAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: OldAdminAddMarksCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assessment, module)
		HibernateHelpers.initialiseAndUnproxy(mandatory(assessment)) match {
			case assignment: Assignment =>
				p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
			case exam: Exam =>
				p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
		}
	}

}

trait OldAdminAddMarksCommandState extends AddMarksCommandState {
	def submitter: CurrentUser
	var updatedReleasedFeedback: Seq[Feedback] = Nil
}
