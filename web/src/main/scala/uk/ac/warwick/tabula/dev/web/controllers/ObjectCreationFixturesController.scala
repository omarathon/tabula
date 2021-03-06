package uk.ac.warwick.tabula.dev.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.dev.web.commands._
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/fixtures/create/module"))
class ModuleCreationFixturesController {

  @ModelAttribute("createModuleCommand")
  def command(): Appliable[Module] = {
    ModuleFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createModuleCommand") cmd: Appliable[Module]): Module = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/groupset"))
class SmallGroupSetCreationFixturesController {

  @ModelAttribute("createGroupSetCommand")
  def command(): Appliable[SmallGroupSet] = {
    SmallGroupSetFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createGroupSetCommand") cmd: Appliable[SmallGroupSet]): View = {
    val newSet = cmd.apply()
    new JSONView(Map("id" -> newSet.id))
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/groupsetMembership"))
class SmallGroupSetMembershipCreationFixturesController {

  @ModelAttribute("createMembershipCommand")
  def command(): Appliable[SmallGroupSet] = {
    GroupsetMembershipFixtureCommand()

  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createMembershipCommand") cmd: Appliable[SmallGroupSet]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/groupEvent"))
class SmallGroupEventCreationFixturesController {

  @ModelAttribute("createEventCommand")
  def command(): Appliable[SmallGroupEvent] = {
    SmallGroupEventFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createEventCommand") cmd: Appliable[SmallGroupEvent]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/groupMembership"))
class SmallGroupMembershipCreationFixturesController {

  @ModelAttribute("createMembershipCommand")
  def command(): Appliable[SmallGroup] = {
    GroupMembershipFixtureCommand()

  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createMembershipCommand") cmd: Appliable[SmallGroup]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/staffMember"))
class StaffMemberCreationFixturesController {

  @ModelAttribute("createMemberCommand")
  def command(): Appliable[StaffMember] = {
    StaffMemberFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createMemberCommand") cmd: Appliable[StaffMember]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/studentMember"))
class StudentMemberCreationFixturesController {

  @ModelAttribute("createMemberCommand")
  def command(): Appliable[StudentMember] = {
    StudentMemberFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createMemberCommand") cmd: Appliable[StudentMember]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/route"))
class RouteCreationFixturesController {

  @ModelAttribute("createRouteCommand")
  def command(): Appliable[Route] = {
    RouteCreationFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createRouteCommand") cmd: Appliable[Route]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/course"))
class CourseCreationFixturesController {

  @ModelAttribute("createCourseCommand")
  def command(): Appliable[Course] = {
    CourseCreationFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createCourseCommand") cmd: Appliable[Course]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/assessmentComponent"))
class AssessmentComponentCreationFixturesController {

  @ModelAttribute("createAssessmentComponentCommand")
  def command(): Appliable[AssessmentComponent] = {
    AssessmentComponentCreationFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createAssessmentComponentCommand") cmd: Appliable[AssessmentComponent]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/upstreamAssessmentGroup"))
class UpstreamAssessmentGroupCreationFixturesController {

  @ModelAttribute("createUpstreamAssessmentGroupCommand")
  def command(): Appliable[UpstreamAssessmentGroup] = {
    UpstreamAssessmentGroupCreationFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createUpstreamAssessmentGroupCommand") cmd: Appliable[UpstreamAssessmentGroup]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/relationship"))
class RelationshipCreationFixturesController {

  @ModelAttribute("createRelationship")
  def command(): Appliable[MemberStudentRelationship] = {
    RelationshipFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createRelationship") cmd: Appliable[MemberStudentRelationship]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/moduleRegistration"))
class ModuleRegistrationFixturesController {

  @ModelAttribute("moduleRegistrationCommand")
  def command(): Appliable[Seq[ModuleRegistration]] = {
    ModuleRegistrationFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("moduleRegistrationCommand") cmd: Appliable[ModuleRegistration]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/update/assignment"))
class UpdateAssignmentFixturesController {

  @ModelAttribute("updateAssignmentCommand")
  def command(): Appliable[Seq[Assignment]] = {
    UpdateAssignmentCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("updateAssignmentCommand") cmd: Appliable[Seq[Assignment]]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/extension"))
class CreateExtensionFixturesController {

  @ModelAttribute("createExtensionCommand")
  def command(): Appliable[Extension] = {
    CreateExtensionFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createExtensionCommand") cmd: Appliable[Extension]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/update/extensionSettings"))
class UpdateExtensionSettingsFixturesController {

  @ModelAttribute("updateExtensionSettingsFixtureCommand")
  def command(): Appliable[Department] = {
    UpdateExtensionSettingsFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("updateExtensionSettingsFixtureCommand") cmd: Appliable[Department]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/attendanceMonitoringScheme"))
class CreateAttendanceMonitoringSchemeFixturesController {

  @ModelAttribute("createAttendanceMonitoringSchemeFixtureCommand")
  def command(): Appliable[AttendanceMonitoringScheme] = {
    AttendanceMonitoringSchemeFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createAttendanceMonitoringSchemeFixtureCommand") cmd: Appliable[AttendanceMonitoringScheme]): Unit = {
    cmd.apply()
  }
}

@Controller
@RequestMapping(Array("/fixtures/create/cm2Assignment"))
class CreateCM2AssignmentFixtureController {

  @ModelAttribute("createCM2AssignmentFixtureCommand")
  def command(): Appliable[Assignment] = {
    CreateCM2AssignmentFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createCM2AssignmentFixtureCommand") cmd: Appliable[Assignment]): Unit = {
    cmd.apply()
  }
}


@Controller
@RequestMapping(Array("/fixtures/create/memberNote"))
class MemberNoteCreationFixturesController {

  @ModelAttribute("createMemberNoteCommand")
  def command(): Appliable[MemberNote] = {
    MemberNoteCreationFixtureCommand()
  }

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("createMemberNoteCommand") cmd: Appliable[MemberNote]): Unit = {
    cmd.apply()
  }
}