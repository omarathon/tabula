package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.services.fileserver.RenderableZip
import uk.ac.warwick.courses.services.ZipService
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.services.AssignmentService

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
@Configurable
class DownloadSubmissionsCommand extends Command[RenderableZip] with ReadOnly with ApplyWithCallback[RenderableZip] {

	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _
	@BeanProperty var filename: String = _

	@BeanProperty var submissions: JList[Submission] = ArrayList()
    @BeanProperty var students: JList[String] = ArrayList()
    
	@Autowired var zipService: ZipService = _
    @Autowired var assignmentService: AssignmentService = _

	override def apply: RenderableZip = {
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

	override def describe(d: Description) = d
		.assignment(assignment)
		.submissions(submissions)
		.studentIds(submissions.map(_.universityId))
		.properties(
			"submissionCount" -> Option(submissions).map(_.size).getOrElse(0))

}