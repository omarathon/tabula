package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import javax.servlet.http.HttpServletResponse

import org.joda.time.DateTime
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.coursework.commands.assignments.AddAssignmentCommand
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{DateFormats, AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.{UpstreamGroupPropertyEditor, UpstreamGroup, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments"))
class ModuleAssignmentsController extends ApiController
	with ListAssignmentsForModuleApi
	with CreateAssignmentApi
	with AssignmentToJsonConverter

trait ListAssignmentsForModuleApi {
	self: ApiController with AssignmentToJsonConverter =>

	@ModelAttribute("listCommand")
	def command(@PathVariable module: Module, user: CurrentUser): ViewViewableCommand[Module] =
		new ViewViewableCommand(Permissions.Module.ManageAssignments, mandatory(module))

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@ModelAttribute("listCommand") command: ViewViewableCommand[Module], errors: Errors, @RequestParam(required = false) academicYear: AcademicYear) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val module = command.apply()
			val assignments = module.assignments.asScala.filter { assignment =>
				!assignment.deleted && (academicYear == null || academicYear == assignment.academicYear)
			}

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"academicYear" -> Option(academicYear).map { _.toString }.orNull,
				"assignments" -> assignments.map(jsonAssignmentObject)
			)))
		}
	}
}

trait AssignmentToJsonConverter {
	self: ApiController =>

	def jsonAssignmentObject(assignment: Assignment): Map[String, Any] = {
		val basicInfo = Map(
			"id" -> assignment.id,
			"archived" -> assignment.archived,
			"academicYear" -> assignment.academicYear.toString,
			"name" -> assignment.name,
			"studentUrl" -> (toplevelUrl + Routes.coursework.assignment(assignment)),
			"collectMarks" -> assignment.collectMarks,
			"markingWorkflow" -> Option(assignment.markingWorkflow).map { mw => Map(
				"id" -> mw.id,
				"name" -> mw.name
			)}.orNull,
			"feedbackTemplate" -> Option(assignment.feedbackTemplate).map { ft => Map(
				"id" -> ft.id,
				"name" -> ft.name
			)}.orNull,
			"summative" -> assignment.summative,
			"dissertation" -> assignment.dissertation
		)

		val submissionsInfo =
			if (assignment.collectSubmissions) {
				Map(
					"collectSubmissions" -> true,
					"displayPlagiarismNotice" -> assignment.displayPlagiarismNotice,
					"restrictSubmissions" -> assignment.restrictSubmissions,
					"allowLateSubmissions" -> assignment.allowLateSubmissions,
					"allowResubmission" -> assignment.allowResubmission,
					"allowExtensions" -> assignment.allowExtensions,
					"fileAttachmentLimit" -> assignment.attachmentLimit,
					"fileAttachmentTypes" -> assignment.fileExtensions,
					"submissionFormText" -> assignment.commentField.map { _.value }.getOrElse(""),
					"wordCountMin" -> assignment.wordCountField.map { _.min }.orNull,
					"wordCountMax" -> assignment.wordCountField.map { _.max }.orNull,
					"wordCountConventions" -> assignment.wordCountField.map { _.conventions }.getOrElse(""),
					"submissions" -> assignment.submissions.size(),
					"unapprovedExtensions" -> assignment.countUnapprovedExtensions
				)
			} else {
				Map(
					"collectSubmissions" -> false
				)
			}


		val membershipInfo = assignment.membershipInfo
		val studentMembershipInfo = Map(
			"studentMembership" -> Map(
				"total" -> membershipInfo.totalCount,
				"linkedSits" -> membershipInfo.sitsCount,
				"included" -> membershipInfo.usedIncludeCount,
				"excluded" -> membershipInfo.usedExcludeCount
			),
			"sitsLinks" -> assignment.upstreamAssessmentGroups.map { uag => Map(
				"moduleCode" -> uag.moduleCode,
				"assessmentGroup" -> uag.assessmentGroup,
				"occurrence" -> uag.occurrence,
				"sequence" -> uag.sequence
			)}
		)

		val datesInfo =
			if (assignment.openEnded) {
				Map(
					"openEnded" -> true,
					"opened" -> assignment.isOpened,
					"closed" -> false,
					"openDate" -> DateFormats.IsoDateTime.print(assignment.openDate)
				)
			} else {
				Map(
					"openEnded" -> false,
					"opened" -> assignment.isOpened,
					"closed" -> assignment.isClosed,
					"openDate" -> DateFormats.IsoDateTime.print(assignment.openDate),
					"closeDate" -> DateFormats.IsoDateTime.print(assignment.closeDate),
					"feedbackDeadline" -> assignment.feedbackDeadline.map(DateFormats.IsoDate.print).orNull
				)
			}

		val countsInfo = Map(
			"feedback" -> assignment.countFullFeedback,
			"unpublishedFeedback" -> assignment.countUnreleasedFeedback
		)

		basicInfo ++ submissionsInfo ++ studentMembershipInfo ++ datesInfo ++ countsInfo
	}
}

trait CreateAssignmentApi {
	self: ApiController =>

	@ModelAttribute("createCommand")
	def command(@PathVariable module: Module): AddAssignmentCommand =
		new AddAssignmentCommand(module)

	@InitBinder(Array("createCommand"))
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def create(@RequestBody request: CreateAssignmentRequest, @ModelAttribute("createCommand") command: AddAssignmentCommand, errors: Errors)(implicit response: HttpServletResponse) = {
		request.copyTo(command, errors)

		globalValidator.validate(command, errors)
		command.validate(errors)
		command.afterBind()

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val assignment = command.apply()

			response.setStatus(HttpStatus.CREATED.value())
			response.addHeader("Location", toplevelUrl + Routes.api.assignment(assignment))
			null
		}
	}
}

class CreateAssignmentRequest extends JsonApiRequest[AddAssignmentCommand]
	with BooleanAssignmentProperties {

	@BeanProperty var name: String = _
	@BeanProperty var openDate: DateTime = _
	@BeanProperty var closeDate: DateTime = _
	@BeanProperty var academicYear: AcademicYear = _
	@BeanProperty var feedbackTemplate: FeedbackTemplate = _
	@BeanProperty var markingWorkflow: MarkingWorkflow = _
	@BeanProperty var includeUsers: JList[String] = JArrayList()
	@BeanProperty var upstreamGroups: JList[UpstreamGroup] = JArrayList()
	@BeanProperty var fileAttachmentLimit: JInteger = 1
	@BeanProperty var fileAttachmentTypes: JList[String] = JArrayList()
	@BeanProperty var minWordCount: JInteger = _
	@BeanProperty var maxWordCount: JInteger = _
	@BeanProperty var wordCountConventions: String = "Exclude any bibliography or appendices."

	override def copyTo(state: AddAssignmentCommand, errors: Errors) {
		state.name = name
		state.openDate = openDate
		state.closeDate = closeDate
		state.academicYear = academicYear
		state.feedbackTemplate = feedbackTemplate
		state.markingWorkflow = markingWorkflow
		state.massAddUsers = includeUsers.asScala.mkString("\n")
		state.upstreamGroups = upstreamGroups
		state.fileAttachmentLimit = fileAttachmentLimit
		state.fileAttachmentTypes = fileAttachmentTypes
		state.wordCountMin = minWordCount
		state.wordCountMax = maxWordCount
		state.wordCountConventions = wordCountConventions
		state.openEnded = openEnded
		state.collectMarks = collectMarks
		state.collectSubmissions = collectSubmissions
		state.restrictSubmissions = restrictSubmissions
		state.allowLateSubmissions = allowLateSubmissions
		state.allowResubmission = allowResubmission
		state.displayPlagiarismNotice = displayPlagiarismNotice
		state.allowExtensions = allowExtensions
		state.summative = summative
		state.dissertation = dissertation
		state.includeInFeedbackReportWithoutSubmissions = includeInFeedbackReportWithoutSubmissions
	}
}