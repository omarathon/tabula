package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.jobs.zips.SubmissionZipFileJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{SubmissionService, ZipService}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

import scala.collection.JavaConverters._


/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSubmissionsCommand(val module: Module, val assignment: Assignment, user: CurrentUser)
	extends Command[Either[RenderableZip, JobInstance]] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)

	var zipService = Wire[ZipService]
	var submissionService = Wire[SubmissionService]
	var jobService = Wire[JobService]

	var filename: String = _
	var submissions: JList[Submission] = JArrayList()
	var students: JList[String] = JArrayList()

	override def applyInternal(): Either[RenderableZip, JobInstance] = {
		if (submissions.isEmpty && students.isEmpty) throw new ItemNotFoundException
		else if (!submissions.isEmpty && !students.isEmpty) throw new IllegalStateException("Only expecting one of students and submissions to be set")
		else if (!students.isEmpty && submissions.isEmpty) {
			submissions = (for (
				uniId <- students.asScala;
				submission <- submissionService.getSubmissionByUniId(assignment, uniId)
			) yield submission).asJava
		}

		if (submissions.asScala.exists(_.assignment != assignment)) {
			throw new IllegalStateException("Submissions don't match the assignment")
		}

		if (submissions.size() < SubmissionZipFileJob.minimumSubmissions) {
			val zip = zipService.getSomeSubmissionsZip(submissions.asScala)
			val renderable = new RenderableZip(zip)
			Left(renderable)
		} else {
			Right(jobService.add(Option(user), SubmissionZipFileJob(submissions.asScala.map(_.id).toSeq)))
		}

	}

	override def describe(d: Description) {

		val downloads: Seq[Submission] = {
			if (students.asScala.nonEmpty) students.asScala.flatMap(submissionService.getSubmissionByUniId(assignment, _))
			else submissions.asScala
		}

		d.assignment(assignment)
		.submissions(downloads)
		.studentIds(downloads.map(_.universityId))
		.properties("submissionCount" -> Option(downloads).map(_.size).getOrElse(0))
	}

}