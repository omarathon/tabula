package uk.ac.warwick.courses.web.controllers.admin

import scala.collection.JavaConversions._

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.courses.data.model.{Assignment, Module}
import uk.ac.warwick.courses.commands.assignments._
import uk.ac.warwick.courses.web.Mav
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.CurrentUser
import org.codehaus.jackson.map.ObjectMapper
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.JavaImports._
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.helpers.DateBuilder
import uk.ac.warwick.courses.actions.Participate
import javax.validation.Valid

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/extensions"))
class ExtensionController extends BaseController{

	import ExtensionController.JSON_CONTENT_TYPE

	@Autowired var assignmentService:AssignmentService =_
	@Autowired var json:ObjectMapper =_
	

	@ModelAttribute
	def addCommand(@PathVariable assignment:Assignment, user:CurrentUser) = new AddExtensionCommand(assignment, user)
	@ModelAttribute
	def editCommand(@PathVariable assignment:Assignment, user:CurrentUser) = new EditExtensionCommand(assignment, user)
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
		val existingExtensions = assignment.extensions

		// users that are members of the assignment but have not yet requested or been granted an extension
		val potentialExtensions = assignmentMembership -- (existingExtensions.map(_.universityId).toSet)

		val model = Mav("admin/assignments/extensions/list",
			"module" -> module,
			"assignment" -> assignment,
			"existingExtensions" -> existingExtensions,
			"potentialExtensions" -> potentialExtensions
		)

		crumbed(model, module)
	}

	@RequestMapping(value=Array("add"), method=Array(GET))
	def addExtension(@PathVariable module:Module, @PathVariable assignment:Assignment,
		@RequestParam("universityId") universityId:String, @ModelAttribute cmd:AddExtensionCommand, errors:Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		val model = Mav("admin/assignments/extensions/add",
			"module" -> module,
			"assignment" -> assignment,
			"universityId" -> universityId
		).noLayout
		model
	}

	@RequestMapping(value=Array("edit/{universityId}"), method=Array(GET))
	def editExtension(@PathVariable module:Module, @PathVariable assignment:Assignment,
		@PathVariable("universityId") universityId:String, @ModelAttribute cmd:EditExtensionCommand, errors:Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))

		val extension = assignment.findExtension(universityId).get
		cmd.copyFromExtension(extension)

		val model = Mav("admin/assignments/extensions/edit",
			"module" -> module,
			"assignment" -> assignment,
			"universityId" -> universityId
		).noLayout
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
		).noLayout
		model
	}

	@RequestMapping(value=Array("{action:add}"), method=Array(POST))
	@ResponseBody
	def persistExtension(@PathVariable module:Module, @PathVariable assignment:Assignment, @PathVariable action:String,
											 @Valid @ModelAttribute cmd:AddExtensionCommand, result:BindingResult,
											 response:HttpServletResponse, errors: Errors){
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		/*if(errors.hasErrors){
			val extensionItem = cmd.extensionItems.last
			addExtension(module, assignment, extensionItem.universityId, cmd, errors)
		}
		else {	*/
			val extensions = cmd.apply()
			val extensionsJson:JList[Map[String, Object]] = toJson(action, extensions)



			// TODO use JSONView once zoe has commited it
			response.setContentType(JSON_CONTENT_TYPE)
			val out = response.getWriter
			json.writeValue(out, extensionsJson)
		//}
	}

	@RequestMapping(value=Array("{action:edit}"), method=Array(POST))
	@ResponseBody
	def persistExtensionEdit(@PathVariable module:Module, @PathVariable assignment:Assignment,
													 @PathVariable action:String, @Valid @ModelAttribute cmd:EditExtensionCommand,
													 result:BindingResult, response:HttpServletResponse, errors: Errors) {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		// TODO implement validation - see commented-out code in persistExtension
		val extensions = cmd.apply()
		val extensionsJson:JList[Map[String, Object]] = toJson(action, extensions)
		// TODO use JSONView once zoe has commited it
		response.setContentType(JSON_CONTENT_TYPE)
		val out = response.getWriter
		json.writeValue(out, extensionsJson)
	}

	val dateBuilder = new DateBuilder

	def toJson(action:String, extensions:List[Extension]) = {

		def toJson(extension:Extension) = Map[String, String](
			"id" -> extension.universityId,
			"expiryDate" -> dateBuilder.format(extension.expiryDate),
			"reason" -> extension.reason,
			"action" -> action
		)

		val extensionJson = extensions.map(toJson(_))
		extensionJson
	}

	@RequestMapping(value=Array("delete"), method=Array(POST))
	@ResponseBody
	def deleteExtension(@PathVariable module:Module, @PathVariable assignment:Assignment,
											 @ModelAttribute cmd:DeleteExtensionCommand, response:HttpServletResponse, errors: Errors){

		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		val universityIds = cmd.apply()

		val deletedExtensionsJson:JList[Map[String, Object]] =
			universityIds.map(universityId => Map[String, String]("id" -> universityId, "action" -> "delete"))

		response.setContentType(JSON_CONTENT_TYPE)
		val out = response.getWriter
		json.writeValue(out, deletedExtensionsJson)
	}

	object ExtensionController {
		val JSON_CONTENT_TYPE = "application/json"
	}

}


