package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.convert.NullableConvertibleConverter
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.ModerationModerator
import uk.ac.warwick.tabula.data.model.{Assignment, Convertible, GradeBoundary, MarkerFeedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._
import scala.util.Try


object MarkerBulkModerationCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser, currentStage: MarkingWorkflowStage, gradeGenerator: GeneratesGradesFromMarks) =
		new MarkerBulkModerationCommandInternal(assignment, marker, submitter, currentStage, gradeGenerator)
			with ComposableCommand[Seq[MarkerFeedback]]
			with MarkerBulkModerationValidation
			with MarkerBulkModerationPermissions
			with MarkerBulkModerationDescription
			with AutowiringCM2MarkingWorkflowServiceComponent
			with AutowiringFeedbackServiceComponent
			with PopulateMarkerFeedbackComponentImpl
}

class MarkerBulkModerationCommandInternal(
	val assignment: Assignment, val marker: User, val submitter: CurrentUser, val currentStage: MarkingWorkflowStage, val gradeGenerator: GeneratesGradesFromMarks
) extends CommandInternal[Seq[MarkerFeedback]] with MarkerBulkModerationState with MarkerBulkModerationValidation {

	self: CM2MarkingWorkflowServiceComponent with FeedbackServiceComponent with PopulateMarkerFeedbackComponent =>

	def applyInternal(): Seq[MarkerFeedback] = {

		// populate any feedback that wasn't moderated
		populateMarkerFeedback(assignment, validForAdjustment(previousMarker).filterNot(_.hasContent))

		val toAdjust = validForAdjustment(previousMarker).map(moderatorFeedback => {

			val preAdjustmentMark = moderatorFeedback.mark
				.getOrElse(throw new IllegalArgumentException(s"No mark to be adjusted for ${moderatorFeedback.student.getUserId}"))

			val amountToAdjust: Double = (adjustmentType match {
				case Percentage => percentageAdjustment.map(percentage => preAdjustmentMark * (percentage / 100))
				case Points => pointAdjustment.map(_.toDouble)
			}).getOrElse(0)

			moderatorFeedback.mark = Some(direction.op(preAdjustmentMark.toDouble, amountToAdjust).ceil.toInt.max(0).min(100)) // round and cap between 0 and 100
			moderatorFeedback
		})

		val newMarks = (for (mf <- toAdjust; mark <- mf.mark) yield mf.student.getWarwickId -> mark).toMap
		val grades = gradeGenerator.applyForMarks(newMarks)

		toAdjust.map(markerFeedback => {
			markerFeedback.grade = grades(markerFeedback.student.getWarwickId).find(_.isDefault).map(_.grade)
			markerFeedback.updatedOn = DateTime.now
			feedbackService.save(markerFeedback)

			if(markerFeedback.feedback.outstandingStages.asScala.forall(_.isInstanceOf[FinalStage])) {
				cm2MarkingWorkflowService.finaliseFeedback(markerFeedback)
			}

			markerFeedback
		})
	}
}

trait MarkerBulkModerationPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: MarkerBulkModerationState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait MarkerBulkModerationValidation extends SelfValidating {
	self: MarkerBulkModerationState =>
	def validate(errors: Errors) {

		if (!previousMarker.isFoundUser) {
			val previousRole = ModerationModerator.previousStages.headOption.map(_.roleName).getOrElse(MarkingWorkflowStage.DefaultRole)
			errors.rejectValue("previousMarker", "markerFeedback.bulkAdjustment.marker", Array(previousRole), "")
		}

		if (!adjustment.hasText) {
			errors.rejectValue("adjustment", "markerFeedback.bulkAdjustment.missing")
		} else if (adjustmentType == Percentage && percentageAdjustment.isEmpty){
			errors.rejectValue("adjustment", "markerFeedback.bulkAdjustment.percentageFormat")
		} else if (adjustmentType == Points && pointAdjustment.isEmpty){
			errors.rejectValue("adjustment", "markerFeedback.bulkAdjustment.pointFormat")
		} else if (pointAdjustment.map(_.toLong).orElse(percentageAdjustment.map(_.round)).exists(l => l < 0 || l > 100) ) {
			errors.rejectValue("adjustment", "markerFeedback.bulkAdjustment.range")
		}

	}
}

trait MarkerBulkModerationDescription extends Describable[Seq[MarkerFeedback]] {
	self: MarkerBulkModerationState =>

	override lazy val eventName: String = "MarkerBulkModeration"

	def describe(d: Description) {
		val students = validForAdjustment(previousMarker).map(_.student)
		d.assignment(assignment)
		d.studentIds(students.map(_.getWarwickId))
		d.studentUsercodes(students.map(_.getUserId))
		d.property("oldMarks", validForAdjustment(previousMarker).map(mf => mf.student.getUserId -> mf.mark).toMap)
		d.property("oldGrades", validForAdjustment(previousMarker).map(mf => mf.student.getUserId -> mf.grade).toMap)
	}

	override def describeResult(d: Description, result: Seq[MarkerFeedback]) {
		d.property("newMarks", result.map(mf => mf.student.getUserId -> mf.mark).toMap)
		d.property("newGrades", result.map(mf => mf.student.getUserId -> mf.grade).toMap)
	}
}

trait MarkerBulkModerationState {

	self: CM2MarkingWorkflowServiceComponent =>

	// context
	def assignment: Assignment
	def gradeGenerator: GeneratesGradesFromMarks
	def submitter: CurrentUser
	def marker: User
	def currentStage: MarkingWorkflowStage

	// bindable variables
	var previousMarker: User = _
	var direction: MarkerAdjustmentDirection = _
	var adjustmentType: MarkerAdjustmentType = _
	var adjustment: String = _

	def percentageAdjustment: Option[Double] = Try(adjustment.toDouble).toOption
	def pointAdjustment: Option[Int] = Try(adjustment.toInt).toOption

	private lazy val markersFeedback = cm2MarkingWorkflowService
		.getAllFeedbackForMarker(assignment, marker).getOrElse(currentStage, Seq())
		.filter(mf => mf.feedback.outstandingStages.asScala.map(_.order).reduceOption(_ max _).getOrElse(0) >= currentStage.order) // not finished marking


	private lazy val markersStudents = markersFeedback.map(_.student).toSet

	// markers from the previous stage that share at least one student with the current marker
	lazy val previousMarkers: Seq[User] = {
		val markersForLastStage = currentStage.previousStages.flatMap(s => assignment.cm2MarkingWorkflow.markers.getOrElse(s, Seq()))
		markersForLastStage.filter(m => {
			cm2MarkingWorkflowService.getAllStudentsForMarker(assignment, m).toSet.intersect(markersStudents).nonEmpty
		})
	}

	private lazy val previousMarkerFeedback: Map[User, Seq[MarkerFeedback]] = previousMarkers.map(marker => { marker ->
		cm2MarkingWorkflowService.getAllFeedbackForMarker(assignment, marker)
			.filterKeys(currentStage.previousStages.contains).values.flatten.toSeq
			.filter(mf => markersStudents.contains(mf.student))
	}).toMap

	private lazy val noMark: Map[User, Seq[User]] =
		previousMarkerFeedback.map { case (marker, markerFeedback) => marker -> markerFeedback.filter(_.mark.isEmpty).map(_.student) }

	private lazy val alreadyPublished: Map[User, Seq[User]] =
		previousMarkerFeedback.map { case (marker, markerFeedback) => marker -> markerFeedback.filter(_.feedback.released).map(_.student) }

	private lazy val noGradeGradeBoundaries: Map[User, Seq[User]] = {
		val preAdjustedGrades: Map[String, Seq[GradeBoundary]] = {
			val marksMap = (for (mf <- previousMarkerFeedback.values.flatten; mark <- mf.mark) yield mf.student.getWarwickId -> mark).toMap
			gradeGenerator.applyForMarks(marksMap)
		}

		previousMarkerFeedback.map { case (marker, markerFeedback) =>
			marker -> markerFeedback.filter(mf => preAdjustedGrades.getOrElse(mf.student.getWarwickId, Seq()).isEmpty).map(_.student)
		}
	}

	private lazy val adjustmentsApplied: Map[User, Seq[User]] = previousMarkerFeedback.map { case (marker, markerFeedback) =>
		marker -> markerFeedback.filter(_.feedback.hasPrivateOrNonPrivateAdjustments).map(_.student)
	}

	lazy val skipReasons: Map[User, Map[User, Seq[String]]] = previousMarkers.map(marker => {
		val mark = noMark.getOrElse(marker, Seq())
		val published = alreadyPublished.getOrElse(marker, Seq())
		val noGrade = noGradeGradeBoundaries.getOrElse(marker, Seq())
		val adjustments = adjustmentsApplied.getOrElse(marker, Seq())

		val allSkipped = (mark ++ published ++ noGrade ++ adjustments).toSet
		val markersReasons = allSkipped.map(student => {
			val reasons = mark.find(_ == student).map(_ => "Feedback has no mark").toSeq ++
				published.find(_ == student).map(_ => "Feedback has already been published").toSeq ++
				noGrade.find(_ == student).map(_ => "No appropriate mark scheme is available from SITS").toSeq ++
				adjustments.find(_ == student).map(_ => "Post-marking adjustments have been made to this feedback").toSeq
			student -> reasons
		}).toMap
		marker -> markersReasons
	}).toMap

	lazy val validForAdjustment: Map[User, Seq[MarkerFeedback]] = previousMarkerFeedback.map { case (marker, markerFeedback) =>
		val invalid = skipReasons.getOrElse(marker, Map()).keySet
		marker -> markerFeedback.map(_.student).filterNot(invalid.contains).flatMap(s => markersFeedback.find(_.student == s))
	}

}

sealed trait MarkerAdjustmentType extends Convertible[String] {
	val description: String
	def getName: String = value // for Spring
	def getDescription: String = description // for Spring
}

case object Percentage extends MarkerAdjustmentType { val value = "percentage"; val description = "%" }
case object Points extends MarkerAdjustmentType { val value = "points"; val description = "Points" }

object MarkerAdjustmentType {
	val values: Seq[MarkerAdjustmentType] = Seq(Points, Percentage)
	implicit val factory: String => MarkerAdjustmentType = { name: String =>
		values.find(_.getName == name).getOrElse(throw new IllegalArgumentException(s"$name not found"))
	}
}

class MarkerAdjustmentTypeConverter extends NullableConvertibleConverter[String, MarkerAdjustmentType]

sealed trait MarkerAdjustmentDirection extends Convertible[String] {
	def getName: String = value // for Spring
	def op: (Double, Double) => Double
}

case object Up extends MarkerAdjustmentDirection { val value = "Up" ; def op: (Double, Double) => Double = (a:Double, b:Double) => a+b }
case object Down extends MarkerAdjustmentDirection { val value = "Down" ; def op: (Double, Double) => Double = (a:Double, b:Double) => a-b }

object MarkerAdjustmentDirection {
	val values: Seq[MarkerAdjustmentDirection] = Seq(Up, Down)
	implicit val factory: String => MarkerAdjustmentDirection = { name: String =>
		values.find(_.getName == name).getOrElse(throw new IllegalArgumentException(s"$name not found"))
	}
}

class MarkerAdjustmentDirectionConverter extends NullableConvertibleConverter[String, MarkerAdjustmentDirection]