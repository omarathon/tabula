package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.ZipService
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.SubmissionService


/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSubmissionsCommand(
		val module: Module, 
		val assignment: Assignment) 
		extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)
	
	var zipService = Wire.auto[ZipService]
	var submissionService = Wire.auto[SubmissionService]

	var filename: String = _
	var submissions: JList[Submission] = JArrayList()
	var students: JList[String] = JArrayList()

	override def applyInternal(): RenderableZip = {
		if (submissions.isEmpty && students.isEmpty) throw new ItemNotFoundException
		else if (!submissions.isEmpty && !students.isEmpty) throw new IllegalStateException("Only expecting one of students and submissions to be set")
		else if (!students.isEmpty && submissions.isEmpty) {
			submissions = for (
				uniId <- students;
				submission <- submissionService.getSubmissionByUniId(assignment, uniId)
			) yield submission
		}
		
		if (submissions.exists(_.assignment != assignment)) {
			throw new IllegalStateException("Submissions don't match the assignment")
		}

		val zip = zipService.getSomeSubmissionsZip(submissions)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) {

		val downloads: Seq[Submission] = {
			if (!students.isEmpty) students.flatMap(submissionService.getSubmissionByUniId(assignment, _))
			else submissions
		}

		d.assignment(assignment)
		.submissions(downloads)
		.studentIds(downloads.map(_.universityId))
		.properties(
		"submissionCount" -> Option(downloads).map(_.size).getOrElse(0))
	}

}