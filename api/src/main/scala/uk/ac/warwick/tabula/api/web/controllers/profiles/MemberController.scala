package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{MemberToJsonConverter, StudentCourseDetailsToJsonConverter, StudentCourseYearDetailsToJsonConverter}
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.commands.profiles.profile.ViewProfileCommand
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{AutowiringScalaFreemarkerConfigurationComponent, JSONView}

@Controller
@RequestMapping(Array("/v1/member/{member}"))
class MemberController extends ApiController
	with GetMemberApi
	with MemberToJsonConverter
	with AutowiringScalaFreemarkerConfigurationComponent

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

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable member: Member): ViewProfileCommand =
		new ViewProfileCommand(user, mandatory(member))

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getMember(@ModelAttribute("getCommand") command: ViewProfileCommand): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"member" -> jsonMemberObject(mandatory(command.apply()))
		)))
	}
}

trait GetAllMemberCoursesApi {
	self: ApiController with StudentCourseDetailsToJsonConverter =>

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable member: Member): ViewProfileCommand =
		new ViewProfileCommand(user, mandatory(member))

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getCourses(@ModelAttribute("getCommand") command: ViewProfileCommand): Mav = {
		val member = mandatory(command.apply())
		val studentCourseDetails = member match {
			case student: StudentMember if canViewProperty(student, "freshStudentCourseDetails") =>
				Some(student.freshStudentCourseDetails)

			case _ => None
		}

		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"studentCourseDetails" -> mandatory(studentCourseDetails).map(jsonStudentCourseDetailsObject)
		)))
	}
}

trait GetMemberCourseApi {
	self: ApiController with StudentCourseDetailsToJsonConverter =>

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable member: Member, @PathVariable studentCourseDetails: StudentCourseDetails): ViewViewableCommand[StudentCourseDetails] = {
		mustBeLinked(studentCourseDetails, member)
		new ViewViewableCommand(Permissions.Profiles.Read.StudentCourseDetails.Core, studentCourseDetails)
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getCourse(@ModelAttribute("getCommand") command: ViewViewableCommand[StudentCourseDetails]): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"studentCourseDetails" -> jsonStudentCourseDetailsObject(mandatory(command.apply()))
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
	def getCourseYear(@ModelAttribute("getCommand") command: ViewViewableCommand[StudentCourseDetails], @PathVariable academicYear: AcademicYear): Mav = {
		val studentCourseDetails = mandatory(command.apply())
		val studentCourseYearDetails =
			if (canViewProperty(studentCourseDetails, "freshStudentCourseYearDetails"))
				Some(studentCourseDetails.freshStudentCourseYearDetails.filter(_.academicYear == academicYear))
			else None

		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"studentCourseYearDetails" -> mandatory(studentCourseYearDetails).map(jsonStudentCourseYearDetailsObject)
		)))
	}
}

