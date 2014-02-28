package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPointSet}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.manage.AddMonitoringPointSetCommand
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController

@Controller
@RequestMapping(value=Array("/manage/{dept}/sets/add/{academicYear}"))
class AddMonitoringPointSetController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(
		@PathVariable dept: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam createType: String,
		@RequestParam(value="existingSet",required=false) existingSet: MonitoringPointSet,
		@RequestParam(value="template",required=false) template: MonitoringPointSetTemplate
	) = createType match {
		case "blank" => AddMonitoringPointSetCommand(user, dept, academicYear, None, None)
		case "template" => AddMonitoringPointSetCommand(user, dept, academicYear, None, Option(template))
		case "copy" => AddMonitoringPointSetCommand(user, dept, academicYear, Option(existingSet), None)
		case _ => throw new IllegalArgumentException
	}

	@RequestMapping(method=Array(GET,HEAD))
	def form(@PathVariable dept: Department, @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSet]], @RequestParam createType: String) = {
		Mav("manage/set/add_form", "createType" -> createType).crumbs(Breadcrumbs.ManagingDepartment(dept))
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
			Redirect("/manage/" + dept.code, "created" -> sets.map{s => s.route.code}.distinct.size)
		}
	}

}
