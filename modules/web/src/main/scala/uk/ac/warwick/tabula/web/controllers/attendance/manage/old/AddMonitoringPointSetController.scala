package uk.ac.warwick.tabula.web.controllers.attendance.manage.old

import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.manage.old.AddMonitoringPointSetCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPointSetTemplate}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@RequestMapping(Array("/attendance/manage/{dept}/2013/sets/add/{academicYear}"))
class AddMonitoringPointSetController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(
		@PathVariable dept: Department,
		@RequestParam createType: String,
		@RequestParam(value="existingSet",required=false) existingSet: MonitoringPointSet,
		@RequestParam(value="template",required=false) template: MonitoringPointSetTemplate
	) = createType match {
		case "blank" => AddMonitoringPointSetCommand(user, dept, AcademicYear(2013), None, None)
		case "template" => AddMonitoringPointSetCommand(user, dept, AcademicYear(2013), None, Option(template))
		case "copy" => AddMonitoringPointSetCommand(user, dept, AcademicYear(2013), Option(existingSet), None)
		case _ => throw new IllegalArgumentException
	}

	@RequestMapping(method=Array(GET,HEAD))
	def form(@PathVariable dept: Department, @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSet]], @RequestParam createType: String) = {
		Mav("attendance/manage/set/add_form", "createType" -> createType).crumbs(Breadcrumbs.Old.ManagingDepartment(dept))
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@PathVariable dept: Department,
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSet]],
		errors: Errors,
		@RequestParam createType: String
	) = {
		if (errors.hasErrors) {
			form(dept, cmd, createType)
		} else {
			val sets = cmd.apply()
			Redirect(Routes.attendance.old.department.manage(dept), "created" -> sets.map{s => s.route.code}.distinct.size)
		}
	}

}
