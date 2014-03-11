package uk.ac.warwick.tabula.coursework.web.controllers.admin


import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.{StudentMember, Assignment, Module}
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions._
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.{ BindingResult, Errors }
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService, RelationshipService}
import uk.ac.warwick.tabula.{JsonHelper, CurrentUser}
import uk.ac.warwick.tabula.data.model.forms.Extension
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.helpers.DateBuilder
import javax.validation.Valid
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.tabula.coursework.web.Routes.admin.assignment.extension


abstract class ExtensionController extends CourseworkController {
	var json = Wire[ObjectMapper]
	var userLookup = Wire[UserLookupService]
	var relationshipService = Wire[RelationshipService]
	var profileService = Wire[ProfileService]

	// Add the common breadcrumbs to the model
	def crumbed(mav: Mav, module: Module)
		= mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	class ExtensionMap(extension: Extension) {
		def asMap: Map[String, String] = {

			def convertDateToString(date: DateTime) =
				Option(date) match {
					case Some(d: DateTime) => DateBuilder.format(d)
					case _ => ""
				}

			def convertDateToMillis(date: DateTime) =
				Option(date) match {
					case Some(d: DateTime) => d.getMillis.toString
					case _ => null
				}

			Map(
				"id" -> extension.universityId,
				"status" -> extension.state.description,
				"requestedExpiryDate" -> convertDateToString(extension.requestedExpiryDate),
				"expiryDate" -> convertDateToString(extension.expiryDate),
				"expiryDateMillis" -> convertDateToMillis(extension.expiryDate),
				"reviewerComments" -> extension.reviewerComments
			)
		}
	}
	import scala.language.implicitConversions
	implicit def asMap(e: Extension) = new ExtensionMap(e)
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions"))
class ListExtensionsController extends ExtensionController {

	@ModelAttribute
	def listCommand(
									 @PathVariable("module") module:Module,
									 @PathVariable("assignment") assignment:Assignment
									 ) = new ListExtensionsCommand(module, assignment, user)

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(cmd: ListExtensionsCommand): Mav = {
		val extensionGraphs = cmd.apply()

		val model = Mav("admin/assignments/extensions/summary",
			"detailUrl" -> Routes.admin.assignment.extension.detail(cmd.assignment),
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"extensionGraphs" -> extensionGraphs,
			"maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar
		)

		crumbed(model, cmd.module)
	}
}


@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions/detail/{universityId}"))
class EditExtensionController extends ExtensionController {

	@ModelAttribute("modifyExtensionCommand")
	def editCommand(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("universityId") universityId: String,
		user: CurrentUser,
		@RequestParam(defaultValue = "") action: String
	) = EditExtensionCommand(module, assignment, universityId, user, action)

	validatesSelf[SelfValidating]

	// view an extension (or request)
	@RequestMapping(method=Array(GET))
	def editExtension(@ModelAttribute("modifyExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState, errors: Errors): Mav = {
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

		val model = Mav("admin/assignments/extensions/detail",
			"command" -> cmd,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> cmd.universityId,
			"student" -> student,
			"studentContext" -> studentContext,
			"userFullName" -> userLookup.getUserByWarwickUniId(cmd.universityId).getFullName,
			"approvalAction" -> cmd.ApprovalAction,
			"rejectionAction" -> cmd.RejectionAction,
			"revocationAction" -> cmd.RevocationAction
		).noLayout()
		model
	}

	@RequestMapping(method=Array(POST))
	@ResponseBody
	def persistExtension(@Valid @ModelAttribute("modifyExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState, result: BindingResult, errors: Errors): Mav = {
		if (errors.hasErrors) {
			editExtension(cmd, errors)
		} else {
			val extensionJson = JsonHelper.toJson(cmd.apply().asMap)
			Mav("ajax_success", "data" -> extensionJson).noLayout()
		}
	}
}


@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions/revoke/{universityId}"))
class DeleteExtensionController extends ExtensionController {

	@ModelAttribute("deleteExtensionCommand")
	def deleteCommand(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("universityId") universityId: String,
		user: CurrentUser
	) = DeleteExtensionCommand(module, assignment, universityId, user)

	// delete a manually created extension item - this revokes the extension
	@RequestMapping(method=Array(POST))
	def deleteExtension(@ModelAttribute("deleteExtensionCommand") cmd: Appliable[Extension] with ModifyExtensionCommandState): Mav = {
		val extensionJson = JsonHelper.toJson(cmd.apply().asMap)
		Mav("ajax_success", "data" -> extensionJson).noLayout()
	}
}