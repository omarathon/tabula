package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object GenerateExamGridAuditCommand {

	type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState

	def apply(cmd: SelectCourseCommand) =
		new GenerateExamGridAuditCommandInternal(cmd.department, cmd.academicYear)
			with ComposableCommand[Unit]
			with GenerateExamGridAuditState
			with GenerateExamGridAuditDescription
			with GenerateExamGridAuditPermissions
			with ReadOnly
		{
			courses = cmd.courses
			routes = cmd.routes
			yearOfStudy = cmd.yearOfStudy
		}

}

class GenerateExamGridAuditCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Unit] {

	override def applyInternal(): Unit = {}
}

trait GenerateExamGridAuditPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridAuditState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridAuditDescription extends Describable[Unit] {
	self: GenerateExamGridAuditState =>

	override lazy val eventName = "GenerateExamGrid"

	override def describe(d: Description) {
		d.department(department)
		 .property("academicYear", academicYear)
		 .property("courses", courses.asScala.map(_.code).mkString(", "))
		 .property("routes", routes.asScala.map(_.code).mkString(", "))
		 .property("yearOfStudy", yearOfStudy)
	}
}

trait GenerateExamGridAuditState extends GenerateExamGridSelectCourseCommandRequest {
	val department: Department
	val academicYear: AcademicYear
}