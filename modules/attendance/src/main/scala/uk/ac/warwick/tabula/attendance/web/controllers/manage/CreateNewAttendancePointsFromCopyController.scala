package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointType, AttendanceMonitoringScheme, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.attendance.commands.manage.{FindPointsResult, SetsFindResultOnCreateNewAttendancePointsFromCopyCommand, FindPointsCommand, CreateNewAttendancePointsFromCopySearchCommandResult, CreateNewAttendancePointsFromCopyCommand, CreateNewAttendancePointsFromCopySearchCommand}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints/copy"))
class CreateNewAttendancePointsFromCopyController extends AttendanceController {

	@Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = _

	@ModelAttribute("allAcademicYears")
	def allAcademicYears() = {
		val thisAcademicYear = AcademicYear.guessByDate(DateTime.now)
		Seq(thisAcademicYear.previous, thisAcademicYear, thisAcademicYear.next)
	}

	@ModelAttribute("allDepartments")
	def allDepartments(@PathVariable department: Department) = {
		(Seq(department) ++ moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage))
			.sortBy(_.name).distinct
	}

	@ModelAttribute("searchCommand")
	def searchCommand(@RequestParam(required = false) searchDepartment: Department, @RequestParam(required = false) searchAcademicYear: AcademicYear) = {
		if (searchDepartment == null || searchAcademicYear == null)
			null
		else
			CreateNewAttendancePointsFromCopySearchCommand(searchDepartment, searchAcademicYear)
	}

	@ModelAttribute("findCommand")
	def findCommand(
		@RequestParam(required = false) searchDepartment: Department,
		@RequestParam(required = false) searchAcademicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	) = {
		if (searchDepartment == null || searchAcademicYear == null)
			null
		else
			FindPointsCommand(searchDepartment, searchAcademicYear, Option(schemes.asScala.head.pointStyle))
	}

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	) =
		CreateNewAttendancePointsFromCopyCommand(department, academicYear, schemes.asScala.toSeq)

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		Mav("manage/copypoints",
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("search"))
	def search(
		@ModelAttribute("searchCommand") searchCommand: Appliable[CreateNewAttendancePointsFromCopySearchCommandResult],
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam searchDepartment: Department, 
		@RequestParam searchAcademicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	) = {
		val searchResult = searchCommand.apply()
		val allSchemes = {
			if (searchResult.schemes.isEmpty)
				searchResult.sets
			else
				searchResult.schemes
		}
		Mav("manage/copypoints",
			"searchDepartment" -> searchDepartment,
			"searchAcademicYear" -> searchAcademicYear,
			"allSchemes" -> allSchemes,
			"isSchemes" -> !searchResult.schemes.isEmpty,
			"allTypes" -> AttendanceMonitoringPointType.values,
			"findResult" -> findCommand.apply(),
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("copy"))
	def copy(
		@ModelAttribute("searchCommand") searchCommand: Appliable[CreateNewAttendancePointsFromCopySearchCommandResult],
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] with SetsFindResultOnCreateNewAttendancePointsFromCopyCommand with SelfValidating,
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam searchDepartment: Department,
		@RequestParam searchAcademicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	) = {
		val searchResult = searchCommand.apply()
		val allSchemes = {
			if (searchResult.schemes.isEmpty)
				searchResult.sets
			else
				searchResult.schemes
		}
		val findCommandResult = findCommand.apply()
		cmd.setFindResult(findCommandResult)
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		if (errors.hasErrors) {
			Mav("manage/copypoints",
				"searchDepartment" -> searchDepartment,
				"searchAcademicYear" -> searchAcademicYear,
				"findCommand" -> findCommand,
				"findResult" -> findCommandResult,
				"allSchemes" -> allSchemes,
				"isSchemes" -> !searchResult.schemes.isEmpty,
				"allTypes" -> AttendanceMonitoringPointType.values,
				"returnTo" -> getReturnTo("")
			).crumbs(
				Breadcrumbs.Manage.Home,
				Breadcrumbs.Manage.Department(department),
				Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
			)
		} else {
			val points = cmd.apply()
			Redirect(
				getReturnTo(""),
				"points" -> points.size.toString,
				"schemes" -> points.map(_.scheme.id).mkString(",")
			)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("cancel"))
	def cancel(@RequestParam schemes: JList[AttendanceMonitoringScheme]) = {
		Redirect(
			getReturnTo(""),
			"schemes" -> schemes.asScala.map(_.id).mkString(",")
		)
	}

}
