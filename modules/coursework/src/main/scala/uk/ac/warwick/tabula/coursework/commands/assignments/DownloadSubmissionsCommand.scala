package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.ZipService
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AssignmentService


/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSubmissionsCommand extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {
	var zipService = Wire.auto[ZipService]
	var assignmentService = Wire.auto[AssignmentService]

	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _
	@BeanProperty var filename: String = _

	@BeanProperty var submissions: JList[Submission] = ArrayList()
	@BeanProperty var students: JList[String] = ArrayList()

	override def applyInternal(): RenderableZip = {
		if (submissions.isEmpty && students.isEmpty) throw new ItemNotFoundException
		else if (!submissions.isEmpty && !students.isEmpty) throw new IllegalStateException("Only expecting one of students and submissions to be set")
		else if (!students.isEmpty && submissions.isEmpty) {
			submissions = for (
				uniId <- students;
				submission <- assignmentService.getSubmissionByUniId(assignment, uniId)
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
			if (!students.isEmpty) students.flatMap(assignmentService.getSubmissionByUniId(assignment, _))
			else submissions
		}

		d.assignment(assignment)
		.submissions(downloads)
		.studentIds(downloads.map(_.universityId))
		.properties(
		"submissionCount" -> Option(downloads).map(_.size).getOrElse(0))
	}

}