package uk.ac.warwick.tabula.reports.web.controllers.smallgroups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.commands.smallgroups.{AllSmallGroupsReportCommand, SmallGroupsByModuleReportCommand, SmallGroupsReportFilters}

@Controller
@RequestMapping(Array("/{department}/{academicYear}/groups/unrecorded/bymodule"))
class UnrecordedSmallGroupsByModuleReportController extends AbstractSmallGroupsByModuleReportController {

	@ModelAttribute("command")
	override def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		SmallGroupsByModuleReportCommand(
			department,
			academicYear,
			AllSmallGroupsReportCommand(department, academicYear, SmallGroupsReportFilters.unrecorded(academicYear)).apply()
		)

	override val pageRenderPath = "unrecordedByModule"
	override val filePrefix: String = "unrecorded-small-groups-by-module"
}
