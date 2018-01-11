package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.joda.time.DateTime
import org.springframework.util.StringUtils
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.UserOrderingByIds._
import uk.ac.warwick.tabula.helpers.{FoundUser, LazyLists}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.docconversion.{AutowiringMarksExtractorComponent, MarkItem, MarksExtractorComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

object MarkerAddMarksCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new MarkerAddMarksCommandInternal(assignment, marker, submitter, gradeGenerator)
			with ComposableCommand[Seq[MarkerFeedback]]
			with AddMarksCommandBindListener
			with MarkerAddMarksPermissions
			with MarkerAddMarksDescription
			with AutowiringFeedbackServiceComponent
			with AutowiringMarksExtractorComponent
			with AutowiringAssessmentMembershipServiceComponent
}

class MarkerAddMarksCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[Seq[MarkerFeedback]] with MarkerAddMarksState {

	self: FeedbackServiceComponent with AssessmentMembershipServiceComponent =>

	def isModified(markItem: MarkItem): Boolean = {
		markItem.currentMarkerFeedback(assignment, marker).exists(cmf => cmf.hasComments || cmf.hasMarkOrGrade)
	}

	def canMark(markItem: MarkItem): Boolean = markItem.currentMarkerFeedback(assignment, marker).isDefined

	def applyInternal(): Seq[MarkerFeedback] = {

		def saveFeedback(markItem: MarkItem) = {
			val currentMarkerFeedback = markItem.currentMarkerFeedback(assignment, marker)
			currentMarkerFeedback.foreach(mf => {
				mf.mark = if(StringUtils.hasText(markItem.actualMark)) Some(markItem.actualMark.toInt) else None
				mf.grade = Option(markItem.actualGrade)
				mf.comments = markItem.feedbackComment
				mf.feedback.updatedDate = DateTime.now
				mf.updatedOn = DateTime.now
				feedbackService.saveOrUpdate(mf.feedback)
				feedbackService.save(mf)
			})
			currentMarkerFeedback
		}

		// persist valid marks
		marks.asScala.filter(_.isValid).flatMap(saveFeedback)
	}
}

trait MarkerAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: MarkerAddMarksState =>

	def permissionsCheck(p: PermissionsChecking) {

		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait MarkerAddMarksDescription extends Describable[Seq[MarkerFeedback]] {

	self: MarkerAddMarksState =>

	override lazy val eventName = "MarkerAddMarks"

	override def describe(d: Description) {
		d.assignment(assignment)
	}

	override def describeResult(d: Description, result: Seq[MarkerFeedback]): Unit = {
		d.assignment(assignment)
		d.studentIds(result.map(_.feedback.studentIdentifier))
	}
}

trait MarkerAddMarksState extends AddMarksState with CanProxy {

	self: AssessmentMembershipServiceComponent with FeedbackServiceComponent =>

	def marker: User
	override def existingMarks: Seq[MarkItem] = (for {
		student <- assessmentMembershipService.determineMembershipUsers(assignment)
		feedback <- assignment.allFeedback.find(_.usercode == student.getUserId)
		(stage, markerFeedback) <- feedback.feedbackByStage.find{case (s, mf) => mf.marker == marker && s.order == feedback.currentStageIndex}
	} yield {
		val markItem = new MarkItem
		markItem.id = Option(student.getWarwickId).getOrElse(student.getUserId)
		markItem.actualMark = markerFeedback.mark.map(_.toString).getOrElse("")
		markItem.actualGrade = markerFeedback.grade.getOrElse("")
		markItem.feedbackComment = markerFeedback.comments.getOrElse("")
		markItem.stage = Some(stage)
		markItem
	}).sortBy(_.user(assignment))

}

trait AddMarksState {

	self: AssessmentMembershipServiceComponent with FeedbackServiceComponent =>

	def assignment: Assignment
	def gradeGenerator: GeneratesGradesFromMarks
	def submitter: CurrentUser

	def existingMarks: Seq[MarkItem] = assessmentMembershipService.determineMembershipUsers(assignment).map(student => {
		val feedback = feedbackService.getStudentFeedback(assignment, student.getUserId)
		val markItem = new MarkItem
		markItem.id = Option(student.getWarwickId).getOrElse(student.getUserId)
		markItem.actualMark = feedback.flatMap(_.actualMark).map(_.toString).getOrElse("")
		markItem.actualGrade = feedback.flatMap(_.actualGrade).getOrElse("")
		markItem.feedbackComment = feedback.flatMap(_.comments).getOrElse("")
		markItem
	}).sortBy(_.user(assignment))

	// bindable
	var file: UploadedFile = new UploadedFile
	var marks: JList[MarkItem] = LazyLists.create[MarkItem]()
}

trait AddMarksCommandBindListener extends BindListener {

	self: AddMarksState with MarksExtractorComponent =>

	final val MAX_MARKS_ROWS: Int = 5000
	final val VALID_FILE_TYPES: Seq[String] = Seq(".xlsx")

	def isModified(markItem: MarkItem): Boolean
	// for marker versions of the AddMarksCommand check that the student belongs to this marker
	def canMark(markItem: MarkItem): Boolean

	override def onBind(result:BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !VALID_FILE_TYPES.exists(s.endsWith))

		if (invalidFiles.nonEmpty) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), VALID_FILE_TYPES.mkString(", ")), "")
			else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "")
		}

		if (!result.hasErrors) { transactional() {
			file.onBind(result)
			file.attached.asScala.filter(_.hasData).foreach(file => {
				try {
					marks.addAll(marksExtractor.readXSSFExcelFile(assignment, file.asByteSource.openStream()))
				} catch { case _: InvalidFormatException => result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "") }
				if (marks.size() > MAX_MARKS_ROWS) {
					result.rejectValue("file", "file.tooManyRows", Array(MAX_MARKS_ROWS.toString), "")
					marks.clear()
				}
			})
		}}
	}

	def postBindValidation(errors: Errors): Unit = {
		val usersSoFar: mutable.Set[User] = mutable.Set()
		marks.asScala.zipWithIndex.foreach { case(mark, i) =>
			errors.pushNestedPath("marks[" + i + "]")
			validateMarkItem(mark, errors)
			mark.isModified = isModified(mark)
			errors.popNestedPath()
		}

		def validateMarkItem(mark:MarkItem, errors: Errors): Unit = {

			def rejectValue(field: String, errorCode: String, errorArgs: Array[AnyRef] = null, defaultMessage: String = ""): Unit = {
				mark.isValid = false
				errors.rejectValue(field, errorCode, errorArgs, defaultMessage)
			}

			// validate id is present, relates to a user and isn't a dupe
			if(!mark.id.hasText){
				rejectValue("id", "NotEmpty")
			} else {
				mark.user(assignment) match {
					case Some(FoundUser(u)) if usersSoFar.contains(u) => rejectValue("id", "id.duplicate.mark")
					case Some(FoundUser(u)) =>
						usersSoFar.add(u)
						if(!canMark(mark)) rejectValue("id", "id.wrong.marker")
					case None => rejectValue("id", "id.userNotFound", Array(mark.id))
				}
			}

			// validate mark (must be int between 0 and 100)
			if(mark.actualMark.hasText){
				try {
					val asInt = mark.actualMark.toInt
					if (asInt < 0 || asInt > 100) {
						rejectValue("actualMark", "actualMark.range")
					}
				} catch { case _ @ (_: NumberFormatException | _: IllegalArgumentException) => rejectValue("actualMark", "actualMark.format") }
			} else if (assignment.module.adminDepartment.assignmentGradeValidation && mark.actualGrade.hasText) {
				rejectValue("actualMark", "actualMark.validateGrade.adjustedGrade")
			}

			// validate grade is department setting is true
			mark.user(assignment).flatMap(u => Option(u.getWarwickId)).foreach(uniId => {
				if (mark.isValid && mark.actualGrade.hasText && assignment.module.adminDepartment.assignmentGradeValidation) {
					val validGrades = gradeGenerator.applyForMarks(Map(uniId -> mark.actualMark.toInt))(uniId)
					if (validGrades.nonEmpty && !validGrades.exists(_.grade == mark.actualGrade)) {
						rejectValue("actualGrade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")))
					}
				}
			})

			// If a row has no mark or grade, we will quietly ignore it
			if (!mark.actualMark.hasText && !mark.actualGrade.hasText) {
				mark.isValid = false
			}

		}
	}

}