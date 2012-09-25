package uk.ac.warwick.courses.web.controllers.admin

import scala.collection.JavaConversions._

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.courses.data.model.{ Assignment, Module }
import uk.ac.warwick.courses.commands.assignments._
import uk.ac.warwick.courses.web.Mav
import org.springframework.validation.{ BindingResult, Errors }
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.CurrentUser
import org.codehaus.jackson.map.ObjectMapper
import uk.ac.warwick.courses.data.model.forms.Extension
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.helpers.DateBuilder
import uk.ac.warwick.courses.actions.Participate
import javax.validation.Valid
import uk.ac.warwick.courses.web.views.JSONView
import org.joda.time.DateTime

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/extensions"))
class ExtensionController extends BaseController{

	@Autowired var assignmentService:AssignmentService =_
	@Autowired var json:ObjectMapper =_

	@ModelAttribute
	def addCommand(@PathVariable assignment:Assignment, user:CurrentUser) = new ModifyExtensionCommand(assignment, user)
	@ModelAttribute
	def deleteCommand(@PathVariable assignment:Assignment, user:CurrentUser) = new DeleteExtensionCommand(assignment, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module)
		= mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	validatesWith{ (form:ModifyExtensionCommand, errors:Errors) =>
		form.validate(errors)
	}

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(@PathVariable module:Module, @PathVariable assignment:Assignment):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		val assignmentMembership = assignmentService.determineMembershipUsers(assignment).map(_.getWarwickId).toSet
		val manualExtensions = assignment.extensions.filter(_.requestedOn == null)
		val extensionRequests = assignment.extensions.filterNot(manualExtensions contains(_))

		// users that are members of the assignment but have not yet requested or been granted an extension
		val potentialExtensions =
			assignmentMembership -- (manualExtensions.map(_.universityId).toSet) -- (extensionRequests.map(_.universityId).toSet)

		val model = Mav("admin/assignments/extensions/list",
			"module" -> module,
			"assignment" -> assignment,
			"existingExtensions" -> manualExtensions,
			"extensionRequests" -> extensionRequests,
			"potentialExtensions" -> potentialExtensions
		)

		crumbed(model, module)
	}

	@RequestMapping(value=Array("add"), method=Array(GET))
	def addExtension(@PathVariable module:Module, @PathVariable assignment:Assignment,
		@RequestParam("universityId") universityId:String, @ModelAttribute cmd:ModifyExtensionCommand, errors:Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		val model = Mav("admin/assignments/extensions/add",
			"module" -> module,
			"assignment" -> assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}

	@RequestMapping(value=Array("edit/{universityId}"), method=Array(GET))
	def editExtension(@PathVariable module:Module, @PathVariable assignment:Assignment,
		@PathVariable("universityId") universityId:String, @ModelAttribute cmd:ModifyExtensionCommand, errors:Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		val extension = assignment.findExtension(universityId).get
		cmd.copyExtensions(List(extension))

		val model = Mav("admin/assignments/extensions/edit",
			"command" -> cmd,
			"module" -> module,
			"assignment" -> assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}

	@RequestMapping(value=Array("review-request/{universityId}"), method=Array(GET))
	def reviewExtensionRequest(@PathVariable module:Module, @PathVariable assignment:Assignment,
					  @PathVariable("universityId") universityId:String, @ModelAttribute cmd:ModifyExtensionCommand, errors:Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		val extension = assignment.findExtension(universityId).get
		cmd.copyExtensions(List(extension))

		val model = Mav("admin/assignments/extensions/review_request",
			"command" -> cmd,
			"module" -> module,
			"assignment" -> assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}

	@RequestMapping(value=Array("delete/{universityId}"), method=Array(GET))
	def deleteExtension(@PathVariable module:Module, @PathVariable assignment:Assignment,
		@PathVariable("universityId") universityId:String, @ModelAttribute cmd:DeleteExtensionCommand):Mav = {

		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		cmd.universityIds.add(universityId)

		val model = Mav("admin/assignments/extensions/delete",
			"module" -> module,
			"assignment" -> assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}

	@RequestMapping(value=Array("{action:add}", "{action:edit}"), method=Array(POST))
	@ResponseBody
	def persistExtension(@PathVariable module:Module, @PathVariable assignment:Assignment, @PathVariable action:String,
											@Valid @ModelAttribute cmd:ModifyExtensionCommand, result:BindingResult,
											response:HttpServletResponse, errors: Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		if(errors.hasErrors){
			val errorList = errors.getFieldErrors
			val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode))))
			val errorJson = Map("status" -> "error", "result" -> errorMap)
			Mav(new JSONView(errorJson))
		}
		else {
			val extensions = cmd.apply()
			val extensionMap = toJson(extensions)
			val extensionsJson = Map("status" -> "success", "action" -> action, "result" -> extensionMap)
			Mav(new JSONView(extensionsJson))
		}
	}

	val dateBuilder = new DateBuilder

	def toJson(extensions:List[Extension]) = {

		def toJson(extension:Extension) = {
			val status = extension match {
				case e if e.approved => "Approved"
				case e if e.rejected => "Rejected"
				case _ => ""
			}

			val expiryDate =  extension.expiryDate match {
				case d:DateTime => dateBuilder.format(extension.expiryDate)
				case _ => ""
			}

			Map(
				"id" -> extension.universityId,
				"status" -> status,
				"expiryDate" -> expiryDate,
				"approvalComments" -> extension.approvalComments
			)
		}

		val extensionMap = Map() ++ extensions.map(e => e.universityId -> toJson(e))
		extensionMap
	}

	@RequestMapping(value=Array("delete"), method=Array(POST))
	@ResponseBody
	def deleteExtension(@PathVariable module:Module, @PathVariable assignment:Assignment,
						@ModelAttribute cmd:DeleteExtensionCommand, response:HttpServletResponse,
						errors: Errors):Mav = {

		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		val universityIds = cmd.apply()
		// rather verbose json structure for a list of ids but mirrors the result structure used by add and edit
		val result = Map() ++ universityIds.map(id => id -> Map("id" -> id))
		val deletedJson = Map("status" -> "success", "action" -> "delete", "result" -> result)
		Mav(new JSONView(deletedJson))
	}

	object ExtensionController {
		val JSON_CONTENT_TYPE = "application/json"
	}

}