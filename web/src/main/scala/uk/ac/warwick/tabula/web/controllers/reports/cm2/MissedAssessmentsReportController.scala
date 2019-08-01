package uk.ac.warwick.tabula.web.controllers.reports.cm2

import java.io.StringWriter

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.reports.cm2._
import uk.ac.warwick.tabula.commands.{ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.reports.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, XmlView}
import uk.ac.warwick.util.csv.GoodCsvDocument

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/coursework/missed"))
class MissedAssessmentsReportController extends ReportsController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringMaintenanceModeServiceComponent with DepartmentScopedController with AcademicYearScopedController {
  validatesSelf[SelfValidating]

  type Command = ComposableCommand[MissedAssessmentsReport]

  override val departmentPermission: Permission = Permissions.Department.Reports

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

  @ModelAttribute("command")
  def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear): Command =
    MissedAssessmentsReportCommand(mandatory(department), mandatory(academicYear))

  @GetMapping(params = Array("!format"))
  def apply(@Valid @ModelAttribute("command") command: Command, bindingResult: BindingResult, @PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear): Mav = {
    Mav("reports/cm2/missed", "errors" -> bindingResult.hasErrors)
      .crumbs(ReportsBreadcrumbs.Coursework.Home(department, academicYear))
      .secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Coursework.missed(department, year)): _*)
  }

  @GetMapping(params = Array("format=html"))
  def html(@Valid @ModelAttribute("command") command: Command): Mav = {
    val report = command.apply()

    Mav("reports/cm2/missed_table", "report" -> report).noLayout()
  }

  @GetMapping(params = Array("format=csv"))
  def csv(@Valid @ModelAttribute("command") command: Command): CSVView = {
    val report = command.apply()

    val writer = new StringWriter
    val csvBuilder = MissedAssessmentsReportCsvExporter
    val doc = new GoodCsvDocument(csvBuilder, null)

    doc.setHeaderLine(true)
    csvBuilder.headers.foreach(doc.addHeaderField)
    report.entities.foreach(doc.addLine)
    doc.write(writer)

    new CSVView("missed.csv", writer.toString)
  }

  @GetMapping(params = Array("format=xlsx"))
  def xlsx(@Valid @ModelAttribute("command") command: Command): ExcelView = {
    val report = command.apply()

    val workbook = new MissedAssessmentsReportXlsxExporter(report).toXLSX

    new ExcelView("missed.xlsx", workbook)
  }

  @GetMapping(params = Array("format=xml"))
  def xml(@Valid @ModelAttribute("command") command: Command): XmlView = {
    val report = command.apply()

    new XmlView(new MissedAssessmentsReportXmlExporter(report).toXML)
  }
}
