package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.{AbstractMonitoringPointSet, MonitoringPointSet}
import uk.ac.warwick.tabula.attendance.commands.AddMonitoringPointSetCommand
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime

@Controller
@RequestMapping(value=Array("/manage/{dept}/sets/add"))
class AddMonitoringPointSetController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(@PathVariable dept: Department, @RequestParam(value="existingSet",required=false) existingSet: AbstractMonitoringPointSet) =
		AddMonitoringPointSetCommand(dept, Option(existingSet))

	@RequestMapping(method=Array(GET,HEAD))
	def form(@PathVariable dept: Department, @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSet]]) = {
		Mav("manage/set/add_form",
			"thisAcademicYear" -> AcademicYear.guessByDate(new DateTime())
		).crumbs(Breadcrumbs.ManagingDepartment(dept))
	}

	@RequestMapping(method=Array(POST))
	def submit(@PathVariable dept: Department, @Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSet]], errors: Errors) = {
		if (errors.hasErrors) {
			form(dept, cmd)
		} else {
			val sets = cmd.apply()
			Redirect("/manage/" + dept.code, "created" -> sets.map{s => s.route.code}.distinct.size)
		}
	}

}
