package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.attendance.commands.manage.{ExcludeType, StaticType, MembershipItem, AddStudentsToSchemeCommand, IncludeType}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model.StudentMember
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.{ProfileService}

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/new/{scheme}/students"))
class AddStudentsToSchemeController extends AttendanceController {

	final val createAndAddPointsString = "createAndAddPoints"
	final val chooseStudentsString = "chooseStudentsString"

	@Autowired var profileService: ProfileService = _

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable scheme: AttendanceMonitoringScheme) =
		AddStudentsToSchemeCommand(scheme)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable scheme: AttendanceMonitoringScheme) = {
		def getStudentMemberForUniversityId(entry: String): Option[StudentMember] =
			profileService.getMemberByUniversityId(entry) match {
				case Some(student: StudentMember) => Some(student)
				case _ => None
			}

		val membershipItems = (scheme.members.staticUserIds.map(getStudentMemberForUniversityId).flatten.map{ member => MembershipItem(member, StaticType)} ++
			scheme.members.excludedUserIds.map(getStudentMemberForUniversityId).flatten.map{ member => MembershipItem(member, ExcludeType)} ++
			scheme.members.includedUserIds.map(getStudentMemberForUniversityId).flatten.map{ member => MembershipItem(member, IncludeType)}
		).sortBy(membershipItem => (membershipItem.member.lastName, membershipItem.member.firstName))

		Mav("manage/liststudents",
			"membershipItems" -> membershipItems,
			"memberCount" -> scheme.members.members.size,
			"createAndAddPointsString" -> createAndAddPointsString,
			"chooseStudentsString" -> chooseStudentsString
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(scheme.department),
			Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array(chooseStudentsString))
	def chooseStudents = {
		Mav("manage/addstudents")
	}

	@RequestMapping(method = Array(POST), params = Array("create"))
	def save(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			form(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		}

	}

	@RequestMapping(method = Array(POST), params = Array(createAndAddPointsString))
	def saveAndAddPoints(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			form(scheme)
		} else {
			val scheme = cmd.apply()
			// TODO change to wherever the add points path is
			Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		}

	}

}
