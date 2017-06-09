package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Submission}

import scala.collection.JavaConverters._

trait SelectedStudentsState {
	def assignment: Assignment
}

trait SelectedStudentsRequest {
	self: SelectedStudentsState =>

	var students: JList[String] = JArrayList()

	def submissions: Seq[Submission] =
		if (students.isEmpty) JArrayList(assignment.submissions).asScala
		else students.asScala.flatMap { s => JArrayList(assignment.submissions).asScala.find(_.usercode == s) }

	def feedbacks: Seq[AssignmentFeedback] =
		if (students.isEmpty) JArrayList(assignment.feedbacks).asScala
		else students.asScala.flatMap { s => JArrayList(assignment.feedbacks).asScala.find(_.usercode == s) }
}
