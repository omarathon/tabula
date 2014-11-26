package uk.ac.warwick.tabula.reports.web.controllers.smallgroups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.commands.smallgroups._


@Controller
@RequestMapping(Array("/{department}/{academicYear}/groups/all"))
class AllSmallGroupsReportController extends AbstractSmallGroupsReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(mandatory(department), mandatory(academicYear), SmallGroupsReportFilters.identity)

	val pageRenderPath = "allsmallgroups"
	val filePrefix = "all-small-group-attendance"

}

@Controller
@RequestMapping(Array("/{department}/{academicYear}/groups/unrecorded"))
class UnrecordedSmallGroupsReportController extends AbstractSmallGroupsReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(mandatory(department), mandatory(academicYear), SmallGroupsReportFilters.unrecorded)

	val pageRenderPath = "unrecorded"
	val filePrefix = "unrecorded-small-group-attendance"

}

@Controller
@RequestMapping(Array("/{department}/{academicYear}/groups/missed"))
class MissedSmallGroupsReportController extends AbstractSmallGroupsReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(mandatory(department), mandatory(academicYear), SmallGroupsReportFilters.missedUnauthorised)

	val pageRenderPath = "missed"
	val filePrefix = "missed-small-group-attendance"

}