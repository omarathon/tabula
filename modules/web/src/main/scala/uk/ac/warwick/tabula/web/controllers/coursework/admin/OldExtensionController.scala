package uk.ac.warwick.tabula.web.controllers.coursework.admin

import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module, StudentMember}
import uk.ac.warwick.tabula.commands.coursework.assignments.extensions._
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService, UserLookupService}
import uk.ac.warwick.tabula.{CurrentUser, JsonHelper}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.DateBuilder
import javax.validation.Valid

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Profile

abstract class OldExtensionController extends OldCourseworkController {
	var json = Wire[ObjectMapper]
	var userLookup = Wire[UserLookupService]
	var relationshipService = Wire[RelationshipService]
	var profileService = Wire[ProfileService]

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

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/extensions"))
class OldListExtensionsForAssignmentController extends OldExtensionController {

	// Add the common breadcrumbs to the model
	def crumbed(mav: Mav, module: Module)
	= mav.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))

	@ModelAttribute
	def listCommand(
		@PathVariable module:Module,
		@PathVariable assignment:Assignment
		) = new ListExtensionsForAssignmentCommand(module, assignment, user)

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(cmd: ListExtensionsForAssignmentCommand, @RequestParam(value="universityId", required=false) universityId: String): Mav = {
		val extensionGraphs = cmd.apply()

		val model = Mav("coursework/admin/assignments/extensions/summary",
			"detailUrl" -> Routes.admin.assignment.extension.detail(cmd.assignment),
			"module" -> cmd.module,
			"extensionToOpen" -> universityId,
			"assignment" -> cmd.assignment,
			"extensionGraphs" -> extensionGraphs,
			"maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar
		)

		crumbed(model, cmd.module)
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/coursework/admin/department/{department}/manage/extensions"))
class OldListAllExtensionsController extends OldExtensionController {

	// Add the common breadcrumbs to the model
	def crumbed(mav: Mav, department: Department)
	= mav.crumbs(Breadcrumbs.Department(department))

	@ModelAttribute
	def listCommand(
		@PathVariable department:Department
		) = new ListAllExtensionsCommand(department)

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(cmd: ListAllExtensionsCommand, @RequestParam(value="universityId", required=false) universityId: String): Mav = {
		val extensionGraphs = cmd.apply()

		val model = Mav("coursework/admin/assignments/extensions/departmentSummary",
			"extensionToOpen" -> universityId,
			"extensionGraphs" -> extensionGraphs,
			"maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar
		)

		crumbed(model, cmd.department)
	}
}

/**
 * FIXME TAB-2426 Is the second mapping here necessary? Is it referred to anywhere? If not, it should be removed.
 * The first mapping is the one that should be used.
 */
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/extensions/detail/{universityId}"))
class OldEditExtensionController extends OldExtensionController {

	@ModelAttribute("modifyExtensionCommand")
	def editCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable universityId: String,
		user: CurrentUser,
		@RequestParam(defaultValue = "") action: String
	) = EditExtensionCommand(module, assignment, universityId, user, action)

	validatesSelf[SelfValidating]

	// view an extension (or request)
	@RequestMapping(method=Array(GET))
	def editExtension(
		@ModelAttribute("modifyExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState,
		errors: Errors
	): Mav = {
		val student = profileService.getMemberByUniversityId(cmd.extension.universityId)

		val studentContext = student match {
			case Some(student: StudentMember) =>
				val relationships = relationshipService.allStudentRelationshipTypes.map { relationshipType =>
					(relationshipType.description, relationshipService.findCurrentRelationships(relationshipType, student))
				}.toMap.filter({case (relationshipType,relations) => relations.length != 0})

				Map(
					"relationships" -> relationships,
					"course" -> student.mostSignificantCourseDetails
				)
			case _ => Map.empty
		}

		val model = Mav("coursework/admin/assignments/extensions/detail",
			"command" -> cmd,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> cmd.universityId,
			"student" -> student,
			"studentContext" -> studentContext,
			"userFullName" -> userLookup.getUserByWarwickUniId(cmd.universityId).getFullName,
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
		@Valid @ModelAttribute("modifyExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState,
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


@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/extensions/revoke/{universityId}"))
class OldDeleteExtensionController extends OldExtensionController {

	@ModelAttribute("deleteExtensionCommand")
	def deleteCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable universityId: String,
		user: CurrentUser
	) = DeleteExtensionCommand(module, assignment, universityId, user)

	// delete a manually created extension item - this revokes the extension
	@RequestMapping(method=Array(POST))
	def deleteExtension(@ModelAttribute("deleteExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState): Mav = {
		val extensionJson = JsonHelper.toJson(cmd.apply().asMap)
		Mav("ajax_success", "data" -> extensionJson).noLayout()
	}
}
