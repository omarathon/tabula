package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.web.controllers.TopLevelUrlAware
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.web.Routes

import scala.util.Try

trait SubmissionToJsonConverter {
	self: TopLevelUrlAware =>

	def jsonSubmissionObject(student: SubmissionAndFeedbackCommand.Student): Map[String, Any] = {
		student.coursework.enhancedSubmission.map { enhancedSubmission =>
			val submission = enhancedSubmission.submission

			Map(
				"id" -> submission.id,
				"downloaded" -> enhancedSubmission.downloaded,
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
						"reportUrl" -> (toplevelUrl + Routes.coursework.admin.assignment.turnitin.report(submission, attachment))
					)}.orNull
				)},
				"submittedDate" -> Option(submission.submittedDate).map(DateFormats.IsoDateTime.print).orNull,
				"wordCount" -> submission.assignment.wordCountField.flatMap(submission.getValue).map { formValue => JInteger(Try(formValue.value.toInt).toOption) }.orNull,
				"suspectPlagiarised" -> submission.suspectPlagiarised
			)
		}.orNull
	}
}
