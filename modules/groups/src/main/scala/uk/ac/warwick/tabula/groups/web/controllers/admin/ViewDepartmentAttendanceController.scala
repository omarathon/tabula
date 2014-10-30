package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.groups.commands.admin.ViewDepartmentAttendanceCommand
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

abstract class AbstractViewDepartmentAttendanceController extends GroupsController {

	hideDeletedItems

	@ModelAttribute("academicYears") def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava

	@RequestMapping(method=Array(GET, HEAD))
	def adminDepartment(@ModelAttribute("adminCommand") cmd: Appliable[Seq[Module]], @PathVariable("department") dept: Department) = {
		Mav("groups/attendance/view_department",
			"modules" -> cmd.apply()
		).crumbs(Breadcrumbs.Department(dept))
	}

}

@Controller
@RequestMapping(value=Array("/admin/department/{department}/attendance"))
class ViewDepartmentAttendanceController extends AbstractViewDepartmentAttendanceController {

	@ModelAttribute("adminCommand")
	def command(@PathVariable department: Department, user: CurrentUser) =
		ViewDepartmentAttendanceCommand(mandatory(department), AcademicYear.guessSITSAcademicYearByDate(DateTime.now), user)

}

@Controller
@RequestMapping(value=Array("/admin/department/{department}/{academicYear}/attendance"))
class ViewDepartmentAttendanceInYearController extends AbstractViewDepartmentAttendanceController {

	@ModelAttribute("adminCommand")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, user: CurrentUser) =
		ViewDepartmentAttendanceCommand(mandatory(department), mandatory(academicYear), user)


}
