package uk.ac.warwick.tabula.dev.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.springframework.web.bind.annotation.RequestMethod.POST
import uk.ac.warwick.tabula.dev.web.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.data.model.forms.Extension

@Controller
@RequestMapping(Array("/fixtures/create/module"))
class ModuleCreationFixturesController {

	@ModelAttribute("createModuleCommand")
	def getCreateModuleCommand(): Appliable[Module] = {
		ModuleFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createModuleCommand") cmd: Appliable[Module]) = {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/groupset"))
class SmallGroupSetCreationFixturesController {

	@ModelAttribute("createGroupSetCommand")
	def getCreateModuleCommand(): Appliable[SmallGroupSet] = {
		SmallGroupSetFixtureCommand()

	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createGroupSetCommand") cmd: Appliable[SmallGroupSet]):View = {
		val newSet = cmd.apply()
		new JSONView(Map("id"->newSet.id))
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/groupsetMembership"))
class SmallGroupSetMembershipCreationFixturesController {

	@ModelAttribute("createMembershipCommand")
	def getCreateModuleCommand(): Appliable[SmallGroupSet] = {
		GroupsetMembershipFixtureCommand()

	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createMembershipCommand") cmd: Appliable[SmallGroupSet]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/groupEvent"))
class SmallGroupEventCreationFixturesController {

	@ModelAttribute("createEventCommand")
	def getCreateEventCommand(): Appliable[SmallGroupEvent] = {
		SmallGroupEventFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createEventCommand") cmd: Appliable[SmallGroupEvent]) {
		cmd.apply()
	}
}
@Controller
@RequestMapping(Array("/fixtures/create/groupMembership"))
class SmallGroupMembershipCreationFixturesController {

	@ModelAttribute("createMembershipCommand")
	def getCreateModuleCommand(): Appliable[SmallGroup] = {
		GroupMembershipFixtureCommand()

	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createMembershipCommand") cmd: Appliable[SmallGroup]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/studentMember"))
class StudentMemberCreationFixturesController {

	@ModelAttribute("createMemberCommand")
	def getCreateModuleCommand(): Appliable[StudentMember] = {
		StudentMemberFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createMemberCommand") cmd: Appliable[StudentMember]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/route"))
class RouteCreationFixturesController {

	@ModelAttribute("createRouteCommand")
	def getCreateRouteCommand(): Appliable[Route] = {
		RouteCreationFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createRouteCommand") cmd: Appliable[Route]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/course"))
class CourseCreationFixturesController {

	@ModelAttribute("createCourseCommand")
	def getCreatecourseCommand(): Appliable[Course] = {
		CourseCreationFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createCourseCommand") cmd: Appliable[Course]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/relationship"))
class RelationshipCreationFixturesController {

	@ModelAttribute("createRelationship")
	def getCreateRelationshipCommand(): Appliable[StudentRelationship] = {
		RelationshipFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createRelationship") cmd: Appliable[StudentRelationship]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/moduleRegistration"))
class ModuleRegistrationFixturesController {

	@ModelAttribute("moduleRegistrationCommand")
	def getModuleRegistrationCommand(): Appliable[Seq[ModuleRegistration]] = {
		ModuleRegistrationFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("moduleRegistrationCommand") cmd: Appliable[ModuleRegistration]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/update/assignment"))
class UpdateAssignmentFixturesController {

	@ModelAttribute("updateAssignmentCommand")
	def getUpdateAssignmentCommand(): Appliable[Seq[Assignment]] = {
		UpdateAssignmentCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("updateAssignmentCommand") cmd: Appliable[Seq[Assignment]]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/extension"))
class CreateExtensionFixturesController {

	@ModelAttribute("createExtensionCommand")
	def getCreateExtensionCommand(): Appliable[Extension] = {
		CreateExtensionFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createExtensionCommand") cmd: Appliable[Extension]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/update/extensionSettings"))
class UpdateExtensionSettingsFixturesController {

	@ModelAttribute("updateExtensionSettingsFixtureCommand")
	def getUpdateExtensionSettingsFixtureCommand(): Appliable[Department] = {
		UpdateExtensionSettingsFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("updateExtensionSettingsFixtureCommand") cmd: Appliable[Department]) {
		cmd.apply()
	}
}
