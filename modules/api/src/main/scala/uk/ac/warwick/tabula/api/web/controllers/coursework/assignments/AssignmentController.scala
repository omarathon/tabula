package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{DateFormats, WorkflowStageHealth}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.coursework.helpers.{CourseworkFilters, CourseworkFilter}
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.JavaImports._

import scala.util.Try

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments/{assignment}"))
class AssignmentController extends ApiController
	with GetAssignmentApi
	with AssignmentToJsonConverter
	with AssignmentStudentToJsonConverter
	with ReplacingAssignmentStudentMessageResolver

trait GetAssignmentApi {
	self: ApiController with AssignmentToJsonConverter with AssignmentStudentToJsonConverter =>

	validatesSelf[SelfValidating]

	@ModelAttribute("getCommand")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults] =
		SubmissionAndFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@Valid @ModelAttribute("getCommand") command: Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults], errors: Errors, @PathVariable assignment: Assignment) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val results = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"assignment" -> jsonAssignmentObject(assignment),
				"genericFeedback" -> assignment.genericFeedback,
				"students" -> results.students.map(jsonAssignmentStudentObject)
			)))
		}
	}

	override def binding[A](binder: WebDataBinder, cmd: A) {
		binder.registerCustomEditor(classOf[CourseworkFilter], new AbstractPropertyEditor[CourseworkFilter] {
			override def fromString(name: String) = CourseworkFilters.of(name)
			override def toString(filter: CourseworkFilter) = filter.getName
		})
	}

}

trait AssignmentStudentToJsonConverter {
	self: AssignmentStudentMessageResolver with ApiController =>

	def jsonAssignmentStudentObject(student: SubmissionAndFeedbackCommand.Student): Map[String, Any] = {
		val userDetails = Map("universityId" -> student.user.getWarwickId)

		val workflowDetails = Map(
			"state" -> Map(
				"progress" -> Map(
					"percentage" -> student.progress.percentage,
					"health" -> WorkflowStageHealth.fromCssClass(student.progress.t).toString,
					"stateCode" -> student.progress.messageCode,
					"state" -> getMessageForStudent(student, student.progress.messageCode)
				),
				"nextStage" -> student.nextStage.map { stage => Map(
					"name" -> stage.toString,
					"action" -> getMessageForStudent(student, stage.actionCode)
				)}.orNull,
				"stages" -> student.stages.values.map { progress => Map(
					"stage" -> progress.stage.toString,
					"action" -> getMessageForStudent(student, progress.stage.actionCode),
					"stateCode" -> progress.messageCode,
					"state" -> getMessageForStudent(student, progress.messageCode),
					"health" -> progress.health.toString,
					"completed" -> progress.completed,
					"preconditionsMet" -> progress.preconditionsMet
				)}
			)
		)

		val submissionDetails = Map(
			"submission" -> student.coursework.enhancedSubmission.map { enhancedSubmission =>
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
		)

		val extensionDetails = Map(
			"extension" -> student.coursework.enhancedExtension.map { enhancedExtension =>
				val extension = enhancedExtension.extension

				Map(
					"id" -> extension.id,
					"state" -> extension.state.description,
					"expired" -> (extension.state == ExtensionState.Approved && !enhancedExtension.within),
					"expiryDate" -> extension.expiryDate.map(DateFormats.IsoDateTime.print).orNull,
					"requestedExpiryDate" -> extension.requestedExpiryDate.map(DateFormats.IsoDateTime.print).orNull
				)
			}.orNull
		)

		val feedbackDetails = Map(
			"feedback" -> student.coursework.enhancedFeedback.map { enhancedFeedback =>
				val feedback = enhancedFeedback.feedback

				Map(
					"id" -> feedback.id,
					"downloaded" -> enhancedFeedback.downloaded,
					"onlineViewed" -> enhancedFeedback.onlineViewed
				)
			}.orNull
		)

		val courseworkDetails = submissionDetails ++ extensionDetails ++ feedbackDetails

		userDetails ++ workflowDetails ++ courseworkDetails
	}
}

trait AssignmentStudentMessageResolver {
	def getMessageForStudent(student: SubmissionAndFeedbackCommand.Student, key: String, args: Object*): String
}

trait ReplacingAssignmentStudentMessageResolver extends AssignmentStudentMessageResolver {
	self: ApiController =>

	def getMessageForStudent(student: SubmissionAndFeedbackCommand.Student, key: String, args: Object*): String = {
		val studentId = student.user.getWarwickId

		val firstMarker =
			student.coursework.enhancedSubmission
				.flatMap { s => Option(s.submission) }
				.flatMap { _.firstMarker }
				.map { _.getWarwickId }
				.getOrElse("first marker")

		val secondMarker =
			student.coursework.enhancedSubmission
				.flatMap { s => Option(s.submission) }
				.flatMap { _.secondMarker }
				.map { _.getWarwickId }
				.getOrElse("second marker")

		getMessage(key, args)
			.replace("[STUDENT]", studentId)
			.replace("[FIRST_MARKER]", firstMarker)
			.replace("[SECOND_MARKER]", secondMarker)
	}
}