package uk.ac.warwick.tabula.api.web.controllers.profiles

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{APIFieldRestriction, MemberToJsonConverter, StudentCourseDetailsToJsonConverter, StudentCourseYearDetailsToJsonConverter}
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.commands.profiles.profile.{ViewMultipleProfileCommand, ViewProfileCommand}
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{AutowiringScalaFreemarkerConfigurationComponent, JSONView}

@Controller
@RequestMapping(value = Array("/v1/member/{member}"), params = Array("!includeStale"))
class MemberController extends ApiController
  with FreshMemberApi
  with MemberToJsonConverter
  with AutowiringScalaFreemarkerConfigurationComponent

@Controller
@RequestMapping(value = Array("/v1/memberProfiles"), params = Array("members"))
class FreshMemberProfilesController extends ApiController
  with MultipleFreshMemberApi
  with MemberToJsonConverter
  with AutowiringScalaFreemarkerConfigurationComponent

@Controller
@RequestMapping(value = Array("/v1/member/{member}"), params = Array("includeStale"))
class StaleMemberController extends ApiController
  with StaleMemberApi
  with MemberToJsonConverter
  with AutowiringScalaFreemarkerConfigurationComponent {
}

@Controller
@RequestMapping(Array("/v1/member/{member}/course"))
class MemberCoursesController extends ApiController
  with GetAllMemberCoursesApi
  with MemberToJsonConverter
  with AutowiringScalaFreemarkerConfigurationComponent

@Controller
@RequestMapping(Array("/v1/member/{member}/course/{studentCourseDetails}"))
class MemberCourseController extends ApiController
  with GetMemberCourseApi
  with MemberToJsonConverter
  with AutowiringScalaFreemarkerConfigurationComponent

@Controller
@RequestMapping(Array("/v1/member/{member}/course/{studentCourseDetails}/{academicYear}"))
class MemberCourseYearController extends ApiController
  with GetMemberCourseYearApi
  with MemberToJsonConverter
  with AutowiringScalaFreemarkerConfigurationComponent

trait GetMemberApi {
  self: ApiController with MemberToJsonConverter =>

  def checkMember(m: Member): Member

  def getCommand(m: Member): ViewProfileCommand.Command

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def getMember(@ModelAttribute("getCommand") command: ViewProfileCommand.Command, @RequestParam(defaultValue = "member") fields: String): Mav =
    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "member" -> jsonMemberObject(checkMember(command.apply()), APIFieldRestriction.restriction("member", fields))
    )))
}

trait MultipleFreshMemberApi {
  self: ApiController with MemberToJsonConverter =>

  def checkMember(m: Member): Member = notStale(mandatory(m))

  def getCommand(members: JList[String]): ViewMultipleProfileCommand.Command =
    ViewMultipleProfileCommand(members, user)

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def getMembers(@ModelAttribute("getCommand") command: ViewMultipleProfileCommand.Command, @RequestParam members: String[], @RequestParam(defaultValue = "member") fields: String): Mav =
    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "members" -> Map(
        command.apply(members).asScala.map(m => (m.universityId, jsonMemberObject(checkMember(m), APIFieldRestriction.restriction("member", fields)))).toSeq: _*
      )
    )))
}


trait FreshMemberApi extends GetMemberApi {
  self: ApiController with MemberToJsonConverter =>

  def checkMember(m: Member): Member = notStale(mandatory(m))

  @ModelAttribute("getCommand")
  def getCommand(@PathVariable member: Member): ViewProfileCommand.Command =
    ViewProfileCommand(member, user)
}

trait StaleMemberApi extends GetMemberApi {
  self: ApiController with MemberToJsonConverter =>

  def checkMember(m: Member): Member = mandatory(m)

  @ModelAttribute("getCommand")
  def getCommand(@PathVariable member: Member): ViewProfileCommand.Command =
    ViewProfileCommand.stale(member, user)
}

trait GetAllMemberCoursesApi {
  self: ApiController with StudentCourseDetailsToJsonConverter =>

  @ModelAttribute("getCommand")
  def getCommand(@PathVariable member: Member): ViewProfileCommand.Command =
    ViewProfileCommand(notStale(mandatory(member)), user)

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def getCourses(@ModelAttribute("getCommand") command: ViewProfileCommand.Command, @RequestParam(defaultValue = "studentCourseDetails") fields: String): Mav = {
    val member = notStale(mandatory(command.apply()))
    val studentCourseDetails = member match {
      case student: StudentMember if canViewProperty(student, "freshStudentCourseDetails") =>
        Some(student.freshStudentCourseDetails)

      case _ => None
    }

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "studentCourseDetails" -> mandatory(studentCourseDetails).map(jsonStudentCourseDetailsObject(_, APIFieldRestriction.restriction("studentCourseDetails", fields)))
    )))
  }
}

trait GetMemberCourseApi {
  self: ApiController with StudentCourseDetailsToJsonConverter =>

  @ModelAttribute("getCommand")
  def getCommand(@PathVariable member: Member, @PathVariable studentCourseDetails: StudentCourseDetails): ViewViewableCommand[StudentCourseDetails] = {
    mustBeLinked(studentCourseDetails, notStale(mandatory(member)))
    new ViewViewableCommand(Permissions.Profiles.Read.StudentCourseDetails.Core, studentCourseDetails)
  }

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def getCourse(@ModelAttribute("getCommand") command: ViewViewableCommand[StudentCourseDetails], @RequestParam(defaultValue = "studentCourseDetails") fields: String): Mav = {
    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "studentCourseDetails" -> jsonStudentCourseDetailsObject(mandatory(command.apply()), APIFieldRestriction.restriction("studentCourseDetails", fields))
    )))
  }
}

trait GetMemberCourseYearApi {
  self: ApiController with StudentCourseYearDetailsToJsonConverter =>

  @ModelAttribute("getCommand")
  def getCommand(@PathVariable member: Member, @PathVariable studentCourseDetails: StudentCourseDetails): ViewViewableCommand[StudentCourseDetails] = {
    mustBeLinked(studentCourseDetails, member)
    new ViewViewableCommand(Permissions.Profiles.Read.StudentCourseDetails.Core, studentCourseDetails)
  }

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def getCourseYear(@ModelAttribute("getCommand") command: ViewViewableCommand[StudentCourseDetails], @PathVariable academicYear: AcademicYear, @RequestParam(defaultValue = "studentCourseYearDetails") fields: String): Mav = {
    val studentCourseDetails = mandatory(command.apply())
    val studentCourseYearDetails =
      if (canViewProperty(studentCourseDetails, "freshStudentCourseYearDetails"))
        Some(studentCourseDetails.freshStudentCourseYearDetails.filter(_.academicYear == academicYear))
      else None

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "studentCourseYearDetails" -> mandatory(studentCourseYearDetails).map(jsonStudentCourseYearDetailsObject(_, APIFieldRestriction.restriction("studentCourseYearDetails", fields)))
    )))
  }
}

