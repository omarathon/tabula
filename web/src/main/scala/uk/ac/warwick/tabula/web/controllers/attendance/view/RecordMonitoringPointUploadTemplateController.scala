package uk.ac.warwick.tabula.web.controllers.attendance.view

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, FiltersStudentsBase, PopulateOnForm}
import uk.ac.warwick.tabula.commands.attendance.view.{FilterMonitoringPointsCommand, FilterMonitoringPointsCommandResult, RecordMonitoringPointTemplateCommand, SetFilterPointsResultOnRecordMonitoringPointCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(value = Array("/attendance/view/{department}/{academicYear}/points/{templatePoint}/record/upload/template"))
class RecordMonitoringPointUploadTemplateController extends BaseController {

  type RecordMonitoringPointTemplateCommand = Appliable[ExcelView] with PopulateOnForm with SetFilterPointsResultOnRecordMonitoringPointCommand

  @ModelAttribute("filterCommand")
  def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
    FilterMonitoringPointsCommand(mandatory(department), mandatory(academicYear), user)

  @ModelAttribute("templateCommand")
  def templateCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable templatePoint: AttendanceMonitoringPoint) =
    RecordMonitoringPointTemplateCommand(department, academicYear, templatePoint, user)

  @RequestMapping
  def getTemplate(
    @ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
    @Valid @ModelAttribute("templateCommand") templateCommand: RecordMonitoringPointTemplateCommand) : ExcelView = {
    val filterResult = filterCommand.apply()
    templateCommand.setFilteredPoints(filterResult)
    templateCommand.populate()
    templateCommand.apply()
  }

}
