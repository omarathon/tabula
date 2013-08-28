package uk.ac.warwick.tabula.dev.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.springframework.web.bind.annotation.RequestMethod.POST
import uk.ac.warwick.tabula.dev.web.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/fixtures/create/module"))
class ModuleCreationFixturesController {

	@ModelAttribute("createModuleCommand")
	def getCreateModuleCommand(): Appliable[Unit] = {
		ModuleFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createModuleCommand") cmd: Appliable[Unit]) = {
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
	def getCreateModuleCommand(): Appliable[Unit] = {
		GroupsetMembershipFixtureCommand()

	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createMembershipCommand") cmd: Appliable[Unit]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/groupEvent"))
class SmallGroupEventCreationFixturesController {

	@ModelAttribute("createEventCommand")
	def getCreateEventCommand(): Appliable[Unit] = {
		SmallGroupEventFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createEventCommand") cmd: Appliable[Unit]) {
		cmd.apply()
	}
}
@Controller
@RequestMapping(Array("/fixtures/create/groupMembership"))
class SmallGroupMembershipCreationFixturesController {

	@ModelAttribute("createMembershipCommand")
	def getCreateModuleCommand(): Appliable[Unit] = {
		GroupMembershipFixtureCommand()

	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createMembershipCommand") cmd: Appliable[Unit]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/studentMember"))
class StudentMemberCreationFixturesController {

	@ModelAttribute("createMemberCommand")
	def getCreateModuleCommand(): Appliable[Unit] = {
		StudentMemberFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createMemberCommand") cmd: Appliable[Unit]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/route"))
class RouteCreationFixturesController {

	@ModelAttribute("createRouteCommand")
	def getCreateRouteCommand(): Appliable[Unit] = {
		RouteCreationFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createRouteCommand") cmd: Appliable[Unit]) {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/course"))
class CourseCreationFixturesController {

	@ModelAttribute("createCourseCommand")
	def getCreatecourseCommand(): Appliable[Unit] = {
		CourseCreationFixtureCommand()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createCourseCommand") cmd: Appliable[Unit]) {
		cmd.apply()
	}
}