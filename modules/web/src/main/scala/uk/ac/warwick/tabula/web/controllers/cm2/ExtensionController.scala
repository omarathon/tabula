package uk.ac.warwick.tabula.web.controllers.cm2

import javax.validation.Valid

import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, _}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{EditExtensionCommand, ListAllExtensionsCommand, _}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.model.{Assignment, Department, StudentMember}
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService, UserLookupService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConverters._

/* abstract class AbstractExtensionController extends CourseworkController {
	var json: ObjectMapper = Wire[ObjectMapper]
	var userLookup: UserLookupService = Wire[UserLookupService]
	var relationshipService: RelationshipService = Wire[RelationshipService]
	var profileService: ProfileService = Wire[ProfileService]

	class ExtensionMap(extension: Extension) {
		def asMap: Map[String, String] = {

			def convertDateToString(date: Option[DateTime]) = date.map(DateBuilder.format).getOrElse("")

			def convertDateToMillis(date: Option[DateTime]) = date.map(_.getMillis.toString).orNull

			Map(
				"id" -> extension.universityId,
				"status" -> extension.state.description,
				"requestedExpiryDate" -> convertDateToString(extension.requestedExpiryDate),
				"expiryDate" -> convertDateToString(extension.expiryDate),
				"expiryDateMillis" -> convertDateToMillis(extension.expiryDate),
				"extensionDuration" -> extension.duration.toString,
				"requestedExtraExtensionDuration" -> extension.requestedExtraDuration.toString,
				"reviewerComments" -> extension.reviewerComments
			)
		}
	}
	import scala.language.implicitConversions
	implicit def asMap(e: Extension): ExtensionMap = new ExtensionMap(e)
}
*/
//trait ExtensionServices extends CourseworkController {	var json: ObjectMapper = Wire[ObjectMapper]
trait ExtensionServices {	var json: ObjectMapper = Wire[ObjectMapper]
	var userLookup: UserLookupService = Wire[UserLookupService]
	var relationshipService: RelationshipService = Wire[RelationshipService]
	var profileService: ProfileService = Wire[ProfileService]

	class ExtensionMap(extension: Extension) {
		def asMap: Map[String, String] = {

			def convertDateToString(date: Option[DateTime]) = date.map(DateBuilder.format).getOrElse("")

			def convertDateToMillis(date: Option[DateTime]) = date.map(_.getMillis.toString).orNull

			Map(
				"id" -> extension.universityId,
				"status" -> extension.state.description,
				"requestedExpiryDate" -> convertDateToString(extension.requestedExpiryDate),
				"expiryDate" -> convertDateToString(extension.expiryDate),
				"expiryDateMillis" -> convertDateToMillis(extension.expiryDate),
				"extensionDuration" -> extension.duration.toString,
				"requestedExtraExtensionDuration" -> extension.requestedExtraDuration.toString,
				"reviewerComments" -> extension.reviewerComments
			)
		}
	}
	import scala.language.implicitConversions
	implicit def asMap(e: Extension): ExtensionMap = new ExtensionMap(e)
}


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/extensions"))
class FilterExtensionsController extends CourseworkController {

	type FilterExtensionsCommand = Appliable[FilterExtensionResults] with FilterExtensionsState

	@ModelAttribute("filterExtensionsCommand")
	def filterCommand() = FilterExtensionsCommand(user)

	@RequestMapping(method=Array(HEAD,GET))
	def viewForm(@ModelAttribute("filterExtensionsCommand") cmd: FilterExtensionsCommand): Mav = {
		val results = cmd.apply()
		Mav(s"$urlPrefix/admin/extensions/list",
			"command" -> cmd,
			"results" -> results
		)
	}

	@RequestMapping(method=Array(POST))
	def listFilterResults(@ModelAttribute("filterExtensionsCommand") cmd: FilterExtensionsCommand): Mav = {
		val results = cmd.apply()
		Mav(s"$urlPrefix/admin/extensions/_filter_results",
			"command" -> cmd,
			"results" -> results
		).noLayout()
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/extensions/{extension}"))
class ExtensionController extends CourseworkController {

	type ExtensionsDetailCommand = Appliable[ExtensionDetail] with ViewExtensionState
	type ModifyExtensionCommand = Appliable[Extension] with ModifyExtensionState

	validatesSelf[SelfValidating]

	@ModelAttribute("extensionDetailCommand")
	def detailCommand(@PathVariable extension: Extension) = ViewExtensionCommand(mandatory(extension))

	@ModelAttribute("modifyExtensionCommand")
	def modifyCommand(@PathVariable extension: Extension) = ModifyExtensionCommand(mandatory(extension), mandatory(user))

	@RequestMapping(method=Array(GET), path=Array("detail"))
	def detail(
		@ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
		@ModelAttribute("modifyExtensionCommand") updateCommand: ModifyExtensionCommand,
		errors: Errors
	): Mav = {
		val detail = detailCommand.apply()
		Mav(s"$urlPrefix/admin/extensions/detail",
			"detail" -> detail,
			"modifyExtensionCommand" -> updateCommand,
			"states" -> ExtensionState
		).noLayout()
	}

	@RequestMapping(method=Array(POST), path=Array("update"))
	def update(
		@ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
		@Valid @ModelAttribute("modifyExtensionCommand") updateCommand: ModifyExtensionCommand,
		result: BindingResult,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			detail(detailCommand, updateCommand, errors)
		} else {
			updateCommand.apply()
			Mav(new JSONView(Map(
				"success" -> true,
				"redirect" -> Routes.admin.extensions()
			)))
		}
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/extensions/{extension}/supporting-file/{filename}"))
class DownloadExtensionAttachmentController extends CourseworkController {

	type DownloadAttachmentCommand = Appliable[Option[RenderableAttachment]] with ModifyExtensionState

	@ModelAttribute("downloadAttachmentCommand")
	def attachmentCommand(@PathVariable extension: Extension, @PathVariable filename: String) =
		DownloadExtensionAttachmentCommand(mandatory(extension), mandatory(filename))

	@RequestMapping(method=Array(GET))
	def supportingFile(
		@ModelAttribute("downloadAttachmentCommand") attachmentCommand: DownloadAttachmentCommand,
		@PathVariable("filename") filename: String
	): RenderableFile = {
		attachmentCommand.apply().getOrElse{ throw new ItemNotFoundException() }
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/manage/extensions"))
class ListExtensionsForAssignmentController extends CourseworkController {
	@ModelAttribute
	def listCommand(@PathVariable assignment:Assignment)
	= new ListExtensionsForAssignmentCommand(assignment.module, assignment, user)
	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(cmd: ListExtensionsForAssignmentCommand, @RequestParam(value="universityId", required=false) universityId: String): Mav = {
		val extensionGraphs = cmd.apply()
		val model = Mav(s"$urlPrefix/admin/extensions/assignmentSummary",
			"extensionToOpen" -> universityId,
			"extensionGraphs" -> extensionGraphs,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar
		)
		model
	}
}
@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/extensions/{universityId}"))
class EditExtensionController extends CourseworkController with ExtensionServices {

//	trait EditExtensionState {
//
//		var isNew: Boolean = _
//
//		var universityId: String =_
//		var assignment: Assignment =_
//		var module: Module =_
//		var submitter: CurrentUser =_
//
//		@WithinYears(maxFuture = 3) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
//		var expiryDate: DateTime =_
//		var reviewerComments: String =_
//		var state: ExtensionState = ExtensionState.Unreviewed
//		var action: String =_
//		var extension: Extension =_
//
//		final val ApprovalAction = "Grant"
//		final val RejectionAction = "Reject"
//		final val RevocationAction = "Revoke"
//		final val UpdateApprovalAction = "Update"
//	}

	type ExtensionsDetailCommand = Appliable[DisplayExtensionDetail] with DisplayExtensionState
	type EditExtensionCommand = Appliable[Extension] with EditExtensionCommandState

//	@ModelAttribute("modifyExtensionCommand")
//	def editCommand(
//		@PathVariable module: Module,
//		@PathVariable assignment: Assignment,
//		@PathVariable universityId: String,
//		user: CurrentUser,
//		@RequestParam(defaultValue = "") action: String
//	) = EditExtensionCommand(module, assignment, universityId, user, action)

	validatesSelf[SelfValidating]

//	@ModelAttribute("extensionDetailCommand")
//	def grantCommand(@PathVariable assignment: Assignment, @PathVariable universityId: String) = GrantExtensionCommand(mandatory(universityId),mandatory(assignment))

	@ModelAttribute("extensionDetailCommand")
	def detailCommand(@PathVariable assignment: Assignment, @PathVariable universityId: String) = DisplayExtensionCommand(mandatory(universityId),mandatory(assignment))

	@ModelAttribute("editExtensionCommand")
	def editCommand(@PathVariable assignment: Assignment, @PathVariable universityId: String, @RequestParam(defaultValue = "") action: String) = EditExtensionCommand(assignment.module, assignment, universityId, user, action)

	@RequestMapping(method=Array(GET), path=Array("detail"))
	def detail(
		@ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
		@ModelAttribute("editExtensionCommand") updateCommand: EditExtensionCommand,
		errors: Errors
	): Mav = {
		val detail = detailCommand.apply()
		val student = profileService.getMemberByUniversityId(updateCommand.universityId).getOrElse(None)
		val studentContext = student match {
			case Some(student: StudentMember) =>
				val relationships = relationshipService.allStudentRelationshipTypes.map { relationshipType =>
					(relationshipType.description, relationshipService.findCurrentRelationships(relationshipType, student))
				}.toMap.filter({case (relationshipType,relations) => relations.nonEmpty})
				Map(
					"relationships" -> relationships,
					"course" -> student.mostSignificantCourseDetails
				)
			case _ => Map.empty
		}
		Mav(s"$urlPrefix/admin/extensions/assignmentdetail",
			"student" -> student,
			"studentContext" -> studentContext,
			"detail" -> detail,
			"modifyExtensionCommand" -> updateCommand,
			"states" -> ExtensionState,
		  "updateAction" -> updateCommand.UpdateApprovalAction,
			"approvalAction" -> updateCommand.ApprovalAction,
			"rejectionAction" -> updateCommand.RejectionAction,
			"revocationAction" -> updateCommand.RevocationAction
		).noLayout()
	}

	@RequestMapping(method=Array(POST), path=Array("detail"))
	def update(
		@PathVariable assignment: Assignment,
		@ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
		@Valid @ModelAttribute("editExtensionCommand") updateCommand: EditExtensionCommand,
		result: BindingResult,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			detail(detailCommand, updateCommand, errors)
		} else {
			updateCommand.apply()
			Mav(new JSONView(Map(
				"redirect" -> Routes.admin.assignment.extensions(assignment),
				"success" -> true
			)))
		}
	}

	// view an extension (or request)
	@RequestMapping(method=Array(GET))
	def editExtension(
		@ModelAttribute("editExtensionCommand") cmd: EditExtensionCommand, // with ModifyExtensionState,
		errors: Errors
	): Mav = {
		val student = profileService.getMemberByUniversityId(cmd.extension.universityId)
		val studentContext = student match {
			case Some(student: StudentMember) =>
				val relationships = relationshipService.allStudentRelationshipTypes.map { relationshipType =>
					(relationshipType.description, relationshipService.findCurrentRelationships(relationshipType, student))
				}.toMap.filter({case (relationshipType,relations) => relations.nonEmpty})
				Map(
					"relationships" -> relationships,
					"course" -> student.mostSignificantCourseDetails
				)
			case _ => Map.empty
		}

		val model = Mav(s"$urlPrefix/admin/extensions/detail",
			"command" -> cmd,
			"module" -> cmd.extension.assignment.module,
			"assignment" -> cmd.extension.assignment,
			"student" -> student,
			"studentContext" -> studentContext,
			"userFullName" -> userLookup.getUserByWarwickUniId(cmd.extension.universityId).getFullName,
			"updateAction" -> cmd.UpdateApprovalAction,
			"approvalAction" -> cmd.ApprovalAction,
			"rejectionAction" -> cmd.RejectionAction,
			"revocationAction" -> cmd.RevocationAction
		).noLayout()

		model
	}
	@RequestMapping(method=Array(POST))
	@ResponseBody
	def persistExtension(
		@Valid @ModelAttribute("editExtensionCommand") cmd: EditExtensionCommand,
		result: BindingResult,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			editExtension(cmd, errors)
		} else {
			val extensionJson = JsonHelper.toJson(cmd.apply().asMap)
			Mav("ajax_success", "data" -> extensionJson).noLayout()
		}
	}
}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/manage/extensions"))
class ListAllExtensionsController extends CourseworkController {
	@ModelAttribute("command")
	def listCommand(@PathVariable department:Department, @RequestParam(value="academicYear", required=false) academicYear: AcademicYear) =
		new ListAllExtensionsCommand(department, Option(academicYear).getOrElse(AcademicYear.guessSITSAcademicYearByDate(new DateTime)))
	@ModelAttribute("academicYears")
	def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava
	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(@ModelAttribute("command") cmd: ListAllExtensionsCommand, @RequestParam(value="universityId", required=false) universityId: String): Mav = {
		val extensionGraphs = cmd.apply()
		val model = Mav(s"$urlPrefix/admin/extensions/departmentSummary",
			"extensionToOpen" -> universityId,
			"extensionGraphs" -> extensionGraphs,
			"maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar
		)
		model
	}
}