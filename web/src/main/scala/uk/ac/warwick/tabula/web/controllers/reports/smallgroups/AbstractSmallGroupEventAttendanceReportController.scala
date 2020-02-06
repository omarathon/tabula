package uk.ac.warwick.tabula.web.controllers.reports.smallgroups

import java.io.StringWriter

import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.reports.ReportsDateFormats
import uk.ac.warwick.tabula.commands.reports.smallgroups._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, JsonHelper}
import uk.ac.warwick.util.csv.GoodCsvDocument

import scala.xml.Elem

abstract class AbstractSmallGroupEventAttendanceReportController
  extends ReportsController
    with DepartmentScopedController
    with AcademicYearScopedController
    with AutowiringUserSettingsServiceComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  validatesSelf[SelfValidating]

  val pageRenderPath: String
  val filePrefix: String

  def filterResults(result: AllSmallGroupEventAttendanceReportCommand.Result): AllSmallGroupEventAttendanceReportCommand.Result

  def urlGeneratorFactory(department: Department): AcademicYear => String

  override val departmentPermission: Permission = Permissions.Department.Reports

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): AllSmallGroupEventAttendanceReportCommand.Command =
    AllSmallGroupEventAttendanceReportCommand(mandatory(department), mandatory(academicYear))

  @ModelAttribute("processor")
  def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): SmallGroupEventAttendanceReportProcessor.Command =
    SmallGroupEventAttendanceReportProcessor(mandatory(department), mandatory(academicYear))

  @RequestMapping
  def page(
    @Valid @ModelAttribute("command") cmd: AllSmallGroupEventAttendanceReportCommand.Command,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    Mav(s"reports/smallgroups/attendance/$pageRenderPath")
      .crumbs(ReportsBreadcrumbs.SmallGroups.Home(department, academicYear))
      .secondCrumbs(academicYearBreadcrumbs(academicYear)(urlGeneratorFactory(department)): _*)
  }

  @PostMapping
  def apply(
    @Valid @ModelAttribute("command") cmd: AllSmallGroupEventAttendanceReportCommand.Command,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    if (errors.hasErrors) Mav(new JSONErrorView(errors))
    else {
      val results = filterResults(cmd.apply())

      val allEvents: Seq[Map[String, String]] = results.map { result =>
        Map(
          "id" -> result.eventWeek.id,
          "moduleCode" -> result.eventWeek.event.group.groupSet.module.code.toUpperCase,
          "setName" -> result.eventWeek.event.group.groupSet.nameWithoutModulePrefix,
          "format" -> result.eventWeek.event.group.groupSet.format.description,
          "groupName" -> result.eventWeek.event.group.name,
          "week" -> result.eventWeek.week.toString,
          "day" -> result.eventWeek.event.day.getAsInt.toString,
          "dayString" -> result.eventWeek.event.day.shortName,
          "eventDate" -> ReportsDateFormats.ReportDate.print(result.eventWeek.date),
          "eventDateISO" -> result.eventWeek.date.toString,
          "location" -> Option(result.eventWeek.event.location).map(_.toString).orNull,
          "tutors" -> result.eventWeek.event.tutors.users.map(u => s"${u.getFullName} (${u.getUserId})").mkString(", "),
          "eventId" -> result.eventWeek.event.id,
          "late" -> result.eventWeek.late.toString,
          "recorded" -> result.recorded.toString,
          "unrecorded" -> result.unrecorded.toString,
          "earliestRecordedAttendance" -> result.earliestRecordedAttendance.map(ReportsDateFormats.ReportDate.print).orNull,
          "earliestRecordedAttendanceISO" -> result.earliestRecordedAttendance.map(_.toString).orNull,
          "latestRecordedAttendance" -> result.latestRecordedAttendance.map(ReportsDateFormats.ReportDate.print).orNull,
          "latestRecordedAttendanceISO" -> result.latestRecordedAttendance.map(_.toString).orNull,
        )
      }

      Mav(new JSONView(Map(
        "events" -> allEvents,
        "reportRangeStartDate" -> ReportsDateFormats.ReportDate.print(cmd.startDate),
        "reportRangeEndDate" -> ReportsDateFormats.ReportDate.print(cmd.endDate),
      )))
    }
  }

  @PostMapping(Array("/download.csv"))
  def csv(
    @ModelAttribute("processor") processor: SmallGroupEventAttendanceReportProcessor.Command,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @RequestParam data: String
  ): CSVView = {
    val processorResult = getProcessorResult(processor, data)

    val writer = new StringWriter
    val csvBuilder = new SmallGroupEventAttendanceReportExporter(processorResult, department)
    val doc = new GoodCsvDocument(csvBuilder, null)

    doc.setHeaderLine(true)
    csvBuilder.headers.foreach(doc.addHeaderField)
    processorResult.foreach(doc.addLine)
    doc.write(writer)

    new CSVView(s"$filePrefix-${department.code}-${ReportsDateFormats.CSVDate.print(processor.startDate)}-${ReportsDateFormats.CSVDate.print(processor.endDate)}.csv", writer.toString)
  }

  @PostMapping(Array("/download.xlsx"))
  def xlsx(
    @ModelAttribute("processor") processor: SmallGroupEventAttendanceReportProcessor.Command,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @RequestParam data: String
  ): ExcelView = {
    val processorResult = getProcessorResult(processor, data)

    val workbook = new SmallGroupEventAttendanceReportExporter(processorResult, department).toXLSX

    new ExcelView(s"$filePrefix-${department.code}-${ReportsDateFormats.CSVDate.print(processor.startDate)}-${ReportsDateFormats.CSVDate.print(processor.endDate)}.xlsx", workbook)
  }

  @PostMapping(Array("/download.xml"))
  def xml(
    @ModelAttribute("processor") processor: SmallGroupEventAttendanceReportProcessor.Command,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @RequestParam data: String
  ): Elem = {
    val processorResult = getProcessorResult(processor, data)

    new SmallGroupEventAttendanceReportExporter(processorResult, department).toXML
  }

  private def getProcessorResult(processor: SmallGroupEventAttendanceReportProcessor.Command, data: String): Seq[SmallGroupEventAttendanceReportProcessorResult] = {
    val request = JsonHelper.fromJson[SmallGroupEventAttendanceReportRequest](data)
    request.copyTo(processor)
    processor.apply()
  }

}

class SmallGroupEventAttendanceReportRequest extends Serializable {

  var events: JList[JMap[String, String]] = JArrayList()

  var reportRangeStartDate: String = _
  var reportRangeEndDate: String = _

  def copyTo(state: SmallGroupEventAttendanceReportProcessorRequest): Unit = {
    state.events = events
    state.startDate = ReportsDateFormats.ReportDate.parseDateTime(reportRangeStartDate).toLocalDate
    state.endDate = ReportsDateFormats.ReportDate.parseDateTime(reportRangeEndDate).toLocalDate
  }
}
