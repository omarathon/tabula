package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{GradeBoundary, AssessmentComponent, Assignment, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssignmentMembershipServiceComponent, AutowiringAssignmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object GenerateGradesFromMarkCommand {
	def apply(module: Module, assignment: Assignment) =
		new GenerateGradesFromMarkCommandInternal(module, assignment)
			with AutowiringAssignmentMembershipServiceComponent
			with ComposableCommand[Map[String, Seq[GradeBoundary]]]
			with GenerateGradesFromMarkPermissions
			with GenerateGradesFromMarkCommandState
			with ReadOnly with Unaudited
}

trait GeneratesGradesFromMarks {
	def applyForMarks(marks: Map[String, Int]): Map[String, Option[String]]
}

class GenerateGradesFromMarkCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[Map[String, Seq[GradeBoundary]]] with GeneratesGradesFromMarks {

	self: GenerateGradesFromMarkCommandState with AssignmentMembershipServiceComponent =>

	lazy val assignmentUpstreamAssessmentGroupMap = assignment.assessmentGroups.asScala.map(group =>
		group -> group.toUpstreamAssessmentGroup(assignment.academicYear)
	).toMap

	override def applyInternal() = {
		val membership = assignmentMembershipService.determineMembershipUsers(assignment)
		val studentMarksMap: Map[User, Int] = studentMarks.asScala
			.filterNot(_._2 == null)
			.flatMap{case(uniID, mark) =>
				membership.find(_.getWarwickId == uniID).map(u => u -> mark.toInt)
			}.toMap

		val studentAssesmentComponentMap: Map[String, AssessmentComponent] = studentMarksMap.flatMap{case(student, _) =>
			assignmentUpstreamAssessmentGroupMap.find { case (group, upstreamGroup) =>
				upstreamGroup.exists(_.members.includesUser(student))
			}.map{ case (group, _) => student.getWarwickId -> group.assessmentComponent}
		}.toMap

		studentMarks.asScala.map{case(uniId, mark) =>
			uniId -> studentAssesmentComponentMap.get(uniId).map(component => assignmentMembershipService.gradesForMark(component, mark)).getOrElse(Seq())
		}.toMap
	}

	override def applyForMarks(marks: Map[String, Int]) = {
		studentMarks = marks.mapValues(m => JInteger(Option(m))).asJava
		val properResult = applyInternal()
		properResult.mapValues(_.headOption.map(_.grade))
	}

}

trait GenerateGradesFromMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateGradesFromMarkCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
	}

}

trait GenerateGradesFromMarkCommandState {
	def module: Module
	def assignment: Assignment

	// Bind variables
	var studentMarks: JMap[String, Integer] = JHashMap()
}
