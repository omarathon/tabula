package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.{ Assignment, Module }
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions._
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages._
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.{ BindingResult, Errors }
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.CurrentUser
import org.codehaus.jackson.map.ObjectMapper
import uk.ac.warwick.tabula.data.model.forms.Extension
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.actions.{Manage, Participate}
import javax.validation.Valid
import uk.ac.warwick.tabula.web.views.JSONView
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/extensions"))
class ExtensionController extends CourseworkController{

	@Autowired var assignmentService:AssignmentService =_
	@Autowired var userLookup: UserLookupService = _
	@Autowired var json:ObjectMapper =_
	
	@ModelAttribute
	def listCommand(@PathVariable module:Module, @PathVariable assignment:Assignment) = new ListExtensionsCommand(module, assignment, user)

	@ModelAttribute
	def addCommand(@PathVariable module:Module, @PathVariable assignment:Assignment, user:CurrentUser) = new ModifyExtensionCommand(module, assignment, user)
	
	@ModelAttribute
	def deleteCommand(@PathVariable module:Module, @PathVariable assignment:Assignment, user:CurrentUser) = new DeleteExtensionCommand(module, assignment, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module)
	= mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	validatesWith{ (form:ModifyExtensionCommand, errors:Errors) =>
		form.validate(errors)
	}

	@RequestMapping(method=Array(HEAD,GET))
	def listExtensions(cmd: ListExtensionsCommand):Mav = {
		val extensionsInfo = cmd.apply()
		
		val model = Mav("admin/assignments/extensions/list",
			"studentNameLookup" -> extensionsInfo.studentNames,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"existingExtensions" -> extensionsInfo.manualExtensions,
			"extensionRequests" -> extensionsInfo.extensionRequests,
			"isExtensionManager" -> extensionsInfo.isExtensionManager,
			"potentialExtensions" -> extensionsInfo.potentialExtensions
		)

		crumbed(model, cmd.module)
	}

	// manually add an extension - requests will not be handled here
	@RequestMapping(value=Array("add"), method=Array(GET))
	def addExtension(@RequestParam("universityId") universityId:String, @ModelAttribute cmd:ModifyExtensionCommand, errors:Errors):Mav = {
		val model = Mav("admin/assignments/extensions/add",
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}

	// edit an existing manually created extension
	@RequestMapping(value=Array("edit/{universityId}"), method=Array(GET))
	def editExtension(@PathVariable("universityId") universityId:String, @ModelAttribute cmd:ModifyExtensionCommand, errors:Errors):Mav = {
		val extension = cmd.assignment.findExtension(universityId).get
		cmd.copyExtensions(List(extension))

		val model = Mav("admin/assignments/extensions/edit",
			"command" -> cmd,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}

	// review an extension request
	@RequestMapping(value=Array("review-request/{universityId}"), method=Array(GET))
	def reviewExtensionRequest(@PathVariable("universityId") universityId:String, @ModelAttribute cmd:ModifyExtensionCommand, errors:Errors):Mav = {		
		val extension = cmd.assignment.findExtension(universityId).get
		
		// FIXME TAB-377 This needs splitting out. Currently we don't check this permission in ModifyExtensionCommand
		mustBeAbleTo(Manage(extension))

		cmd.copyExtensions(List(extension))

		val model = Mav("admin/assignments/extensions/review_request",
			"command" -> cmd,
			"extension" ->  extension,
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}

	// delete a manually created extension item - this revokes the extension
	@RequestMapping(value=Array("delete/{universityId}"), method=Array(GET))
	def deleteExtension(@PathVariable("universityId") universityId:String, @ModelAttribute cmd:DeleteExtensionCommand):Mav = {
		cmd.universityIds.add(universityId)

		val model = Mav("admin/assignments/extensions/delete",
			"module" -> cmd.module,
			"assignment" -> cmd.assignment,
			"universityId" -> universityId
		).noLayout()
		model
	}


	@RequestMapping(value=Array("{action:add}", "{action:edit}"), method=Array(POST))
	@ResponseBody
	def persistExtension(@PathVariable("action") action:String,
						 @Valid @ModelAttribute cmd:ModifyExtensionCommand, result:BindingResult,
						 response:HttpServletResponse, errors: Errors):Mav = {
		if(errors.hasErrors){
			val errorList = errors.getFieldErrors
			val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode))))
			val errorJson = Map("status" -> "error", "result" -> errorMap)
			Mav(new JSONView(errorJson))
		} else {
			val extensions = cmd.apply()
			extensions.foreach(sendPersistExtensionMessage(_, action))
			val extensionMap = toJson(extensions)
			val extensionsJson = Map("status" -> "success", "action" -> action, "result" -> extensionMap)
			Mav(new JSONView(extensionsJson))
		}
	}

	def sendPersistExtensionMessage(extension: Extension, action:String) = {
		if (extension.isManual){
			if (action == "add") {
				val message = new ExtensionGrantedMessage(extension, extension.userId)
				message.apply()
			}
			else if (action == "edit") {
				val message = new ExtensionChangedMessage(extension, extension.userId)
				message.apply()
			}
		} else {
			if (extension.approved) {
				val message = new ExtensionRequestApprovedMessage(extension, extension.userId)
				message.apply()
			}
			else if (extension.rejected) {
				val message = new ExtensionRequestRejectedMessage(extension, extension.userId)
				message.apply()
			}
		}
		false // TODO what if request was modified
	}

	def toJson(extensions:List[Extension]) = {

		def toJson(extension:Extension) = {
			val status = extension match {
				case e if e.approved => "Approved"
				case e if e.rejected => "Rejected"
				case _ => ""
			}

			val expiryDate =  extension.expiryDate match {
				case d:DateTime => DateBuilder.format(extension.expiryDate)
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
	def deleteExtension(@ModelAttribute cmd:DeleteExtensionCommand, response:HttpServletResponse,
						errors: Errors):Mav = {
		val universityIds = cmd.apply()
		// send messages
		universityIds.foreach(id => {
			val user = userLookup.getUserByWarwickUniId(id)
			val message = new ExtensionDeletedMessage(cmd.assignment, user.getUserId)
			message.apply()
		})
		// rather verbose json structure for a list of ids but mirrors the result structure used by add and edit
		val result = Map() ++ universityIds.map(id => id -> Map("id" -> id))
		val deletedJson = Map("status" -> "success", "action" -> "delete", "result" -> result)
		Mav(new JSONView(deletedJson))
	}

	object ExtensionController {
		val JSON_CONTENT_TYPE = "application/json"
	}

}