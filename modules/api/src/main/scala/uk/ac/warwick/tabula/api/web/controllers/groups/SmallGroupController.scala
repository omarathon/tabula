package uk.ac.warwick.tabula.api.web.controllers.groups

import javax.servlet.http.HttpServletResponse

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors

import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.groups.SmallGroupController.CreateSmallGroupsCommand
import uk.ac.warwick.tabula.commands.groups.admin.EditSmallGroupsCommand.NewGroupProperties
import uk.ac.warwick.tabula.commands.groups.admin._


import uk.ac.warwick.tabula.api.web.controllers.ApiController

import uk.ac.warwick.tabula.api.web.helpers.{AssessmentMembershipInfoToJsonConverter, SmallGroupEventToJsonConverter, SmallGroupToJsonConverter, SmallGroupSetToJsonConverter}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewSet
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import scala.beans.BeanProperty
import scala.collection.JavaConverters._


object SmallGroupController {
	type CreateSmallGroupsCommand = Appliable[Seq[SmallGroup]]  with EditSmallGroupsCommandState with EditSmallGroupsValidation
}


abstract class SmallGroupController  extends ApiController with SmallGroupSetToJsonConverter
	with SmallGroupToJsonConverter	with SmallGroupEventToJsonConverter
	with AssessmentMembershipInfoToJsonConverter with  GetSmallGroupApiFullOutput {
	validatesSelf[SelfValidating]
}

trait GetSmallGroupApiOutput {
	def outputJson(smallGroupSet: SmallGroupSet): Map[String, Any]
}

trait GetSmallGroupApiFullOutput extends GetSmallGroupSetApiOutput {
	self: ApiController with SmallGroupSetToJsonConverter  =>

	def outputJson(smallGroupSet: SmallGroupSet) = Map(
		"academicYear" -> smallGroupSet.academicYear.toString,
		"groupSet" -> jsonSmallGroupSetObject(new ViewSet(smallGroupSet, smallGroupSet.groups.asScala.sorted, GroupsViewModel.Tutor))
	)

}

@Controller
@RequestMapping(Array("/v1/module/{module}/groups/edit/{smallGroupSet}/groups"))
class CreateSmallGroupsControllerForApi extends SmallGroupController  with CreateSmallGroupsApi


trait CreateSmallGroupsApi {
	self: SmallGroupController =>

	@ModelAttribute("command")
	def createCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet): CreateSmallGroupsCommand =
		EditSmallGroupsCommand(module, smallGroupSet)

	def getSmallGroupMav(command: CreateSmallGroupsCommand, smallGroupSet: SmallGroupSet, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok"
			) ++ outputJson(smallGroupSet)))
		}
	}

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def createGroups(@RequestBody request: CreateSmallGroupsRequest, @ModelAttribute("command") command: CreateSmallGroupsCommand, errors: Errors, @PathVariable smallGroupSet: SmallGroupSet)(implicit response: HttpServletResponse) = {
		request.copyTo(command, errors)
		globalValidator.validate(command, errors)
		command.validate(errors)
		getSmallGroupMav(command, smallGroupSet, errors)
	}
}


@Controller
@RequestMapping(Array("/v1/module/{module}/groups/edit/{smallGroupSet}/group/{smallGroup}"))
class EditSmallGroupControllerForApi extends SmallGroupController  with EditSmallGroupApi

trait EditSmallGroupApi {
	self: SmallGroupController =>

	@ModelAttribute("editCommand")
	def editCommand(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup): ModifySmallGroupCommand.Command =
		ModifySmallGroupCommand.edit(module, smallGroupSet, smallGroup)

	def getSmallGroupMav(command: ModifySmallGroupCommand.Command, smallGroupSet: SmallGroupSet, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok"
			) ++ outputJson(smallGroupSet)))
		}
	}

	@RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def editGroup(@RequestBody request: ModifySmallGroupRequest, @ModelAttribute("editCommand") command: ModifySmallGroupCommand.Command, errors: Errors, @PathVariable smallGroupSet: SmallGroupSet, @PathVariable smallGroup: SmallGroup)(implicit response: HttpServletResponse) = {
		request.copyTo(command, errors)
		globalValidator.validate(command, errors)
		command.validate(errors)
		getSmallGroupMav(command, smallGroupSet, errors)
	}
}


class CreateSmallGroupsRequest extends JsonApiRequest[CreateSmallGroupsCommand] {
	@BeanProperty var newGroups: JList[NewGroupProperties] = null

	override def copyTo(state: CreateSmallGroupsCommand, errors: Errors) {
		Option(newGroups).foreach {
			state.newGroups = _
		}
	}
}

class ModifySmallGroupRequest extends JsonApiRequest[ModifySmallGroupCommand.Command] {
	@BeanProperty var name: String = null
	@BeanProperty var maxGroupSize: JInteger = null

	override def copyTo(state: ModifySmallGroupCommand.Command, errors: Errors) {
		Option(name).foreach {
			state.name = _
		}
		Option(maxGroupSize).foreach {
			state.maxGroupSize = _
		}
	}
}
