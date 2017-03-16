package uk.ac.warwick.tabula.data.model.markingworkflow

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.helpers.StringUtils

sealed abstract class MarkingWorkflowStage(val name: String, val order: Int) {
	def roleName: String = "Marker"
	def verb: String = "mark"

	def previousStages: Seq[MarkingWorkflowStage] = Nil
	def nextStages: Seq[MarkingWorkflowStage] = Nil

	override def toString: String = name
}


/**
	* Stages model the steps in any given workflow.
	*
	* If your creating a new worklow that has concurrent stages in the second or later step you need to consider the
	* implications of trying to call CM2MarkingWorkflowService.returnFeedback when on one of those concurrent stages.
	* The model and services can cope with this operation but it will reset for both of the concurrent markers.
	* Perhaps in these cases it should be an admin only operation.
	*/
object MarkingWorkflowStage {

	abstract class FinalStage(n: String) extends MarkingWorkflowStage(name = n, order = Int.MaxValue)

	// single marker workflow
	case object SingleMarker extends MarkingWorkflowStage("SingleMarker", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(SingleMarkingCompleted)
	}
	case object SingleMarkingCompleted extends FinalStage("SingleMarkingCompleted") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(SingleMarker)
	}

	// double blind workflow
	case object DblBlndInitialMarkerA extends MarkingWorkflowStage("DblBlndInitialMarkerA", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
	}
	case object DblBlndInitialMarkerB extends MarkingWorkflowStage("DblBlndInitialMarkerB", 1) {
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
	}
	case object DblBlndFinalMarker extends MarkingWorkflowStage("DblBlndFinalMarker", 2) {
		override def roleName = "Final marker"
		override def verb = "finalise"
		override def nextStages: Seq[MarkingWorkflowStage] = Seq(DblBlndCompleted)
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerA)
	}
	case object DblBlndCompleted extends FinalStage("DblBlndCompleted") {
		override def previousStages: Seq[MarkingWorkflowStage] = Seq(DblBlndFinalMarker)
	}

	val values: Set[MarkingWorkflowStage] = Set(SingleMarker, DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblBlndFinalMarker)

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