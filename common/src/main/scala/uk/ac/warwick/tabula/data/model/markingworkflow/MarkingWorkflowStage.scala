package uk.ac.warwick.tabula.data.model.markingworkflow

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._

sealed abstract class MarkingWorkflowStage(val name: String, val order: Int) {
	def roleName: String = "Marker"
	def verb: String = "mark"
	// used when stages have their own allocations rather than allocations being at the roleName level
	def allocationName: String = roleName
	// used to describe a stage - when two stages share a role name (the same person is responsible for both stages) these will need to be distinct
	def description: String = roleName

	def previousStages: Seq[MarkingWorkflowStage] = Nil
	def nextStages: Seq[MarkingWorkflowStage] = Nil

	// get a description of the next stage - default works best when there is only one next stage - may need overriding in other cases
	def nextStagesDescription: Option[String] = nextStages.headOption.map(_.description)

	override def toString: String = name
}

abstract class FinalStage(n: String) extends MarkingWorkflowStage(name = n, order = Int.MaxValue) {
	override def roleName: String = "Admin"
}

/**
	* Stages model the steps in any given workflow.
	*
	* If your creating a new workflow that has concurrent stages in the second or later step you need to consider the
	* implications of trying to call CM2MarkingWorkflowService.returnFeedback when on one of those concurrent stages.
	* The model and services can cope with this operation but it will reset for both of the concurrent markers.
	* Perhaps in these cases it should be an admin only operation.
	*/
object MarkingWorkflowStage {

	// single marker workflow
	case object SingleMarker extends MarkingWorkflowStage("SingleMarker", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(SingleMarkingCompleted)
	}
	case object SingleMarkingCompleted extends FinalStage("SingleMarkingCompleted") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(SingleMarker)
	}

	// double marker workflow
	case object DblFirstMarker extends MarkingWorkflowStage("DblFirstMarker", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblSecondMarker)
	}
	case object DblSecondMarker extends MarkingWorkflowStage("DblSecondMarker", 2) {
		override def roleName: String = "Second marker"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblFinalMarker)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblFirstMarker)
	}
	case object DblFinalMarker extends MarkingWorkflowStage("DblFinalMarker", 3) {
		override def verb: String = "finalise"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblCompleted)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblSecondMarker)
		override def description: String = "Final marker"
	}
	case object DblCompleted extends FinalStage("DblCompleted") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblFinalMarker)
	}

	// double blind workflow
	case object DblBlndInitialMarkerA extends MarkingWorkflowStage("DblBlndInitialMarkerA", 1) {
		override def roleName: String = "Independent marker"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
		override def allocationName = "Independent marker A"
		override def description: String = allocationName
	}
	case object DblBlndInitialMarkerB extends MarkingWorkflowStage("DblBlndInitialMarkerB", 1) {
		override def roleName: String = "Independent marker"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
		override def allocationName = "Independent marker B"
		override def description: String = allocationName
	}
	case object DblBlndFinalMarker extends MarkingWorkflowStage("DblBlndFinalMarker", 2) {
		override def roleName = "Final marker"
		override def verb = "finalise"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndCompleted)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerA)
		override def description = "Final marker"
	}
	case object DblBlndCompleted extends FinalStage("DblBlndCompleted") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
	}

	// moderated workflow
	case object ModerationMarker extends MarkingWorkflowStage("ModerationMarker", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(ModerationModerator)
	}
	case object ModerationModerator extends MarkingWorkflowStage("ModerationModerator", 2) {
		override def roleName = "Moderator"
		override def verb: String = "moderate"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(ModerationCompleted)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(ModerationMarker)
	}
	case object ModerationCompleted extends FinalStage("ModerationCompleted") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(ModerationModerator)
	}

	// lame manual collection. Keep in sync with the case objects above
	// Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
	def values: Set[MarkingWorkflowStage] = Set(
		SingleMarker, SingleMarkingCompleted,
		DblFirstMarker, DblSecondMarker, DblFinalMarker, DblCompleted,
		DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblBlndFinalMarker, DblBlndCompleted,
		ModerationMarker, ModerationModerator, ModerationCompleted
	)

	def fromCode(code: String): MarkingWorkflowStage =
		if (code == null) null
		else values.find{_.name == code} match {
			case Some(stage) => stage
			case None => throw new IllegalArgumentException(s"Invalid marking stage: $code")
		}

	implicit val defaultOrdering = new Ordering[MarkingWorkflowStage] {
		def compare(a: MarkingWorkflowStage, b: MarkingWorkflowStage): Int = {
			val orderCompare = a.order compare b.order
			if (orderCompare != 0) orderCompare else StringUtils.AlphaNumericStringOrdering.compare(a.name, b.name)
		}
	}
}

class MarkingWorkflowStageUserType extends AbstractBasicUserType[MarkingWorkflowStage, String]{

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): MarkingWorkflowStage = MarkingWorkflowStage.fromCode(string)
	override def convertToValue(stage: MarkingWorkflowStage): String = stage.name
}

class StringToMarkingWorkflowStage extends TwoWayConverter[String, MarkingWorkflowStage] {
	override def convertRight(source: String): MarkingWorkflowStage = source.maybeText.map(MarkingWorkflowStage.fromCode).getOrElse(throw new IllegalArgumentException)
	override def convertLeft(source: MarkingWorkflowStage): String = Option(source).map { _.name }.orNull
}