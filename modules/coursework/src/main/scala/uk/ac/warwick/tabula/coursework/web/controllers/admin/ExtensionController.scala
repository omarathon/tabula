package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentMember, Assignment, Module}
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions._
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.{ BindingResult, Errors }
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService, RelationshipService}
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser}
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.tabula.data.model.forms.Extension
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.helpers.{Logging, DateBuilder}
import javax.validation.Valid
import uk.ac.warwick.tabula.web.views.JSONView
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes


abstract class ExtensionController extends CourseworkController {
	var json = Wire[ObjectMapper]
	var userLookup = Wire[UserLookupService]
	var relationshipService = Wire[RelationshipService]
	var profileService = Wire[ProfileService]

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module)
		= mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	def toJson(extensions: Seq[Extension]) = {
		def toJson(extension:Extension) = {
			val expiryDate =  extension.expiryDate match {
				case d:DateTime => DateBuilder.format(extension.expiryDate)
				case _ => ""
			}

			Map(
				"id" -> extension.universityId,
				"status" -> extension.state.description,
				"expiryDate" -> expiryDate,
				"reviewerComments" -> extension.reviewerComments
			)
		}

		val extensionMap = Map() ++ extensions.map(e => e.universityId -> toJson(e))
		extensionMap
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions"))
class ExtensionRequestSummaryController extends ExtensionController {

	@ModelAttribute
	def listCommand(
									 @PathVariable("module") module:Module,
									 @PathVariable("assignment") assignment:Assignment
									 ) = new ListExtensionsCommand(module, assignment, user)

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(cmd: ListExtensionsCommand):Mav = {
		val extensionGraphs = cmd.apply()

		val model = Mav("admin/assignments/extensions/summary",
			"detailUrl" -> Routes.admin.assignment.extension.detail(cmd.assignment),
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"extensionGraphs" -> extensionGraphs
		)

		crumbed(model, cmd.module)
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions/detail/{universityId}"))
class ExtensionRequestDetailController extends ExtensionController with Logging {

	@ModelAttribute("modifyExtensionCommand")
	def editCommand(
									 @PathVariable("module") module: Module,
									 @PathVariable("assignment") assignment: Assignment,
									 @PathVariable("universityId") universityId: String,
									 user: CurrentUser,
									 @RequestParam(defaultValue = "") action: String) =
		new EditExtensionCommand(module, assignment, universityId, assignment.findExtension(universityId), user, action)

	validatesSelf[EditExtensionCommand]

	// review an extension request
	@RequestMapping(method=Array(GET))
	def reviewExtension(@ModelAttribute("modifyExtensionCommand") cmd: EditExtensionCommand, errors: Errors): Mav = {

		logger.warn("I am here, so why aren't you?")

		val user = userLookup.getUserByWarwickUniId(cmd.targetUniversityId)
		val student = profileService.getMemberByUniversityId(cmd.targetUniversityId)
		val studentRelationships = relationshipService.allStudentRelationshipTypes

		val extraInfo = student match {
			case Some(student: StudentMember) => {
				val relationships = studentRelationships.map { relationshipType =>
					(relationshipType.description, relationshipService.findCurrentRelationships(relationshipType, student))
				}.toMap

				Map(
					"relationships" -> relationships.filter({case (relationshipType,relations) => relations.length != 0}),
					"student" -> student
				) ++ student.mostSignificantCourseDetails.map { scd =>
					Map("studentCourseDetails" -> scd)
				}.getOrElse(Map())
			}
			case _ => Map()
		}

		Mav("admin/assignments/extensions/detail", Map(
			"command" -> cmd,
			"extension" ->  cmd.extension,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> cmd.targetUniversityId,
			"email" -> user.getEmail(),
			"userFullName" -> user.getFullName()) ++ extraInfo
		).noLayout()
	}

	@RequestMapping(method=Array(POST))
	@ResponseBody
	def persistExtensionRequest(@Valid @ModelAttribute("modifyExtensionCommand") cmd:EditExtensionCommand, result:BindingResult,
															response:HttpServletResponse, errors: Errors):Mav = {
		if (errors.hasErrors) {
			val errorList = errors.getFieldErrors
			val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode))))
			val errorJson = Map("status" -> "error", "result" -> errorMap)
			Mav(new JSONView(errorJson))
		} else {
			val extensions = cmd.apply()
			val extensionMap = toJson(extensions)
			val extensionsJson = Map("status" -> "success", "action" -> "edit", "result" -> extensionMap)
			Mav(new JSONView(extensionsJson))
		}
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions/add"))
class AddExtensionController extends ExtensionController {

	@ModelAttribute("modifyExtensionCommand")
	def addCommand(@PathVariable("module") module:Module, @PathVariable("assignment") assignment:Assignment, user:CurrentUser, @RequestParam(defaultValue="") action: String) =
		new AddExtensionCommand(module, assignment, user, action)

	validatesSelf[AddExtensionCommand]

	// manually add an extension - requests will not be handled here
	@RequestMapping(method=Array(GET))
	def addExtension(
		@RequestParam("universityId") universityId:String,
		@ModelAttribute("modifyExtensionCommand") cmd:AddExtensionCommand,
		errors:Errors
	):Mav = {
		val model = Mav("admin/assignments/extensions/add",
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> universityId,
			"userFullName" -> userLookup.getUserByWarwickUniId(universityId).getFullName
		).noLayout()
		model
	}

	@RequestMapping(method=Array(POST))
	@ResponseBody
	def persistExtension(@Valid @ModelAttribute("modifyExtensionCommand") cmd:AddExtensionCommand, result:BindingResult,
						 response:HttpServletResponse, errors: Errors):Mav = {
		if (errors.hasErrors) {
			val errorList = errors.getFieldErrors
			val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode))))
			val errorJson = Map("status" -> "error", "result" -> errorMap)
			Mav(new JSONView(errorJson))
		} else {
			val extensions = cmd.apply()

			val extensionMap = toJson(extensions)
			val extensionsJson = Map("status" -> "success", "action" -> "add", "result" -> extensionMap)
			Mav(new JSONView(extensionsJson))
		}
	}

}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions/edit/{universityId}"))
class EditExtensionController extends ExtensionController {

	@ModelAttribute("modifyExtensionCommand")
	def editCommand(
			@PathVariable("module") module:Module,
			@PathVariable("assignment") assignment:Assignment,
			@PathVariable("universityId") universityId:String,
			user:CurrentUser,
			@RequestParam(defaultValue = "") action: String) =
		new EditExtensionCommand(module, assignment, universityId, assignment.findExtension(universityId), user, action)

	validatesSelf[EditExtensionCommand]

	// edit an existing manually created extension
	@RequestMapping(method=Array(GET))
	def editExtension(@ModelAttribute("modifyExtensionCommand") cmd:EditExtensionCommand, errors:Errors):Mav = {
		val model = Mav("admin/assignments/extensions/edit",
			"command" -> cmd,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> cmd.targetUniversityId,
			"userFullName" -> userLookup.getUserByWarwickUniId(cmd.targetUniversityId).getFullName
		).noLayout()
		model
	}

	@RequestMapping(method=Array(POST))
	@ResponseBody
	def persistExtension(@Valid @ModelAttribute("modifyExtensionCommand") cmd:EditExtensionCommand, result:BindingResult,
						 response:HttpServletResponse, errors: Errors):Mav = {
		if (errors.hasErrors) {
			val errorList = errors.getFieldErrors
			val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode))))
			val errorJson = Map("status" -> "error", "result" -> errorMap)
			Mav(new JSONView(errorJson))
		} else {
			val extensions = cmd.apply()
			val extensionMap = toJson(extensions)
			val extensionsJson = Map("status" -> "success", "action" -> "add", "result" -> extensionMap)
			Mav(new JSONView(extensionsJson))
		}
	}

}


@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/extensions/delete/{universityId}"))
class DeleteExtensionController extends ExtensionController {

	@ModelAttribute
	def deleteCommand(
		@PathVariable("module") module:Module,
		@PathVariable("assignment") assignment:Assignment,
		@PathVariable("universityId") universityId:String,
		user:CurrentUser
	) = new DeleteExtensionCommand(module, assignment, universityId, user)

	// delete a manually created extension item - this revokes the extension
	@RequestMapping(method=Array(GET))
	def deleteExtension(@ModelAttribute cmd:DeleteExtensionCommand):Mav = {
		val student = userLookup.getUserByWarwickUniId(cmd.universityId)
		val model = Mav("admin/assignments/extensions/delete",
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> cmd.universityId,
			"extension" -> cmd.assignment.findExtension(cmd.universityId).getOrElse(""),
			"userFullName" -> student.getFullName,
			"userFirstName" -> student.getFirstName
		).noLayout()
		model
	}

	@RequestMapping(method=Array(POST))
	@ResponseBody
	def deleteExtension(@ModelAttribute cmd: DeleteExtensionCommand, response:HttpServletResponse,
						errors: Errors):Mav = {
		val universityIds = cmd.apply().map { _.universityId }

		// rather verbose json structure for a list of ids but mirrors the result structure used by add and edit
		val result = Map() ++ universityIds.map(id => id -> Map("id" -> id))
		val deletedJson = Map("status" -> "success", "action" -> "delete", "result" -> result)
		Mav(new JSONView(deletedJson))
	}

}