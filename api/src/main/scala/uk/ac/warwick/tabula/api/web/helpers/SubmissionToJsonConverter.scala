package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{DateFormats, TopLevelUrlComponent}

import scala.util.Try

trait SubmissionToJsonConverter {
	self: TopLevelUrlComponent =>

	def jsonSubmissionObject(submission: Submission): Map[String, Any] = {
		Map(
			"id" -> submission.id,
			"late" -> submission.isLate,
			"authorisedLate" -> submission.isAuthorisedLate,
			"attachments" -> submission.allAttachments.map { attachment => Map(
				"filename" -> attachment.name,
				"id" -> attachment.id,
				"originalityReport" -> Option(attachment.originalityReport).map { report => Map(
					"similarity" -> JInteger(report.similarity),
					"overlap" -> JInteger(report.overlap),
					"webOverlap" -> JInteger(report.webOverlap),
					"studentOverlap" -> JInteger(report.studentOverlap),
					"publicationOverlap" -> JInteger(report.publicationOverlap),
					"reportUrl" -> (toplevelUrl + Routes.cm2.admin.assignment.turnitin.report(submission.assignment, attachment.originalityReport))
				)}.orNull
			)},
			"submittedDate" -> Option(submission.submittedDate).map(DateFormats.IsoDateTime.print).orNull,
			"wordCount" -> submission.assignment.wordCountField.flatMap(submission.getValue).map { formValue => JInteger(Try(formValue.value.toInt).toOption) }.orNull,
			"suspectPlagiarised" -> submission.suspectPlagiarised
		)
	}

	def jsonSubmissionObject(student: SubmissionAndFeedbackCommand.Student): Map[String, Any] = {
		student.coursework.enhancedSubmission.map { enhancedSubmission =>
			jsonSubmissionObject(enhancedSubmission.submission) ++ Map(
				"downloaded" -> enhancedSubmission.downloaded
			)
		}.orNull
	}
}
