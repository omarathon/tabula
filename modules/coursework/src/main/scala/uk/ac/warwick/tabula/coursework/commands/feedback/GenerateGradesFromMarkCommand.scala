package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{GeneratesGradesFromMarks, AssignmentMembershipServiceComponent, AutowiringAssignmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object GenerateGradesFromMarkCommand {
	def apply(module: Module, assessment: Assessment) =
		new GenerateGradesFromMarkCommandInternal(module, assessment)
			with AutowiringAssignmentMembershipServiceComponent
			with ComposableCommand[Map[String, Seq[GradeBoundary]]]
			with GenerateGradesFromMarkPermissions
			with GenerateGradesFromMarkCommandState
			with ReadOnly with Unaudited
}

class GenerateGradesFromMarkCommandInternal(val module: Module, val assessment: Assessment)
	extends CommandInternal[Map[String, Seq[GradeBoundary]]] with GeneratesGradesFromMarks {

	self: GenerateGradesFromMarkCommandState with AssignmentMembershipServiceComponent =>

	lazy val assignmentUpstreamAssessmentGroupMap = assessment.assessmentGroups.asScala.toSeq.map(group =>
		group -> group.toUpstreamAssessmentGroup(assessment.academicYear)
	).toMap

	private def isNotNullAndInt(intString: String): Boolean = {
		if (intString == null) {
			false
		} else {
			try {
				intString.toInt
				true
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
					false
			}
		}
	}

	override def applyInternal() = {
		val membership = assessmentMembershipService.determineMembershipUsers(assessment)
		val studentMarksMap: Map[User, Int] = studentMarks.asScala
			.filter(s => isNotNullAndInt(s._2))
			.flatMap{case(uniID, mark) =>
				membership.find(_.getWarwickId == uniID).map(u => u -> mark.toInt)
			}.toMap

		val studentAssesmentComponentMap: Map[String, AssessmentComponent] = studentMarksMap.flatMap{case(student, _) =>
			assignmentUpstreamAssessmentGroupMap.find { case (group, upstreamGroup) =>
				upstreamGroup.exists(_.members.includesUser(student))
			}.map{ case (group, _) => student.getWarwickId -> group.assessmentComponent}
		}.toMap

		studentMarks.asScala.map{case(uniId, mark) =>
			uniId -> studentAssesmentComponentMap.get(uniId).map(component => assessmentMembershipService.gradesForMark(component, mark.toInt)).getOrElse(Seq())
		}.toMap
	}

	override def applyForMarks(marks: Map[String, Int]) = {
		studentMarks = marks.mapValues(m => m.toString).asJava
		applyInternal()
	}

}

trait GenerateGradesFromMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateGradesFromMarkCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assessment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assessment)
	}

}

trait GenerateGradesFromMarkCommandState {
	def module: Module
	def assessment: Assessment

	// Bind variables
	var studentMarks: JMap[String, String] = JHashMap()
}
