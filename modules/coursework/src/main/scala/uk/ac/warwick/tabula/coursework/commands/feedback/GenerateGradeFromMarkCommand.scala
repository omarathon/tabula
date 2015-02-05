package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Assignment, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssignmentMembershipServiceComponent, AutowiringAssignmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object GenerateGradeFromMarkCommand {
	def apply(module: Module, assignment: Assignment) =
		new GenerateGradeFromMarkCommandInternal(module, assignment)
			with AutowiringAssignmentMembershipServiceComponent
			with ComposableCommand[Map[String, Option[String]]]
			with GenerateGradeFromMarkPermissions
			with GenerateGradeFromMarkCommandState
			with ReadOnly with Unaudited
}

trait GeneratesGradesFromMarks {
	def applyForMarks(marks: Map[String, Int]): Map[String, Option[String]]
}

class GenerateGradeFromMarkCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[Map[String, Option[String]]] with GeneratesGradesFromMarks {

	self: GenerateGradeFromMarkCommandState with AssignmentMembershipServiceComponent =>

	override def applyInternal() = {
		val membership = assignmentMembershipService.determineMembershipUsers(assignment)
		val studentMarksMap: Map[User, Int] = studentMarks.asScala.flatMap{case(uniID, mark) =>
			membership.find(_.getWarwickId == uniID).map(u => u -> mark.toInt)
		}.toMap

		val studentAssesmentComponentMap: Map[String, AssessmentComponent] = studentMarksMap.flatMap{case(student, _) =>
			assignment.assessmentGroups.asScala.find(
				_.toUpstreamAssessmentGroup(assignment.academicYear).exists(_.members.includesUser(student))
			).map(group => student.getWarwickId -> group.assessmentComponent)
		}.toMap

		studentMarks.asScala.map{case(uniId, mark) =>
			uniId -> studentAssesmentComponentMap.get(uniId).flatMap(component => assignmentMembershipService.gradeForMark(component, mark))
		}.toMap
	}

	override def applyForMarks(marks: Map[String, Int]) = {
		studentMarks = marks.mapValues(m => JInteger(Option(m))).asJava
		applyInternal()
	}

}

trait GenerateGradeFromMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateGradeFromMarkCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
	}

}

trait GenerateGradeFromMarkCommandState {
	def module: Module
	def assignment: Assignment

	// Bind variables
	var studentMarks: JMap[String, Integer] = JHashMap()
}
