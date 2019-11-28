package uk.ac.warwick.tabula.web.controllers.reports.attendancemonitoring

import java.io.StringWriter

import freemarker.template.{Configuration, DefaultObjectWrapper}
import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.reports.ReportsDateFormats
import uk.ac.warwick.tabula.commands.reports.attendancemonitoring._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.{IntervalFormatter, LazyMaps}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, JsonHelper}
import uk.ac.warwick.util.csv.GoodCsvDocument

import scala.jdk.CollectionConverters._
import scala.xml.Elem

abstract class AbstractAttendanceReportController extends ReportsController
  with DepartmentScopedController with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  validatesSelf[SelfValidating]

  override val departmentPermission: Permission = Permissions.Department.Reports

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

  def command(department: Department, academicYear: AcademicYear): AllAttendanceReportCommand.CommandType

  val pageRenderPath: String
  val filePrefix: String

  def urlGeneratorFactory(department: Department): (AcademicYear) => String

  type AttendanceReportProcessor = Appliable[AttendanceReportProcessorResult] with AttendanceReportProcessorState

  @ModelAttribute("processor")
  def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
    AttendanceReportProcessor(mandatory(department), mandatory(academicYear))

  @RequestMapping(method = Array(GET))
  def page(
    @Valid @ModelAttribute("command") cmd: AllAttendanceReportCommand.CommandType,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    Mav(s"reports/attendancemonitoring/$pageRenderPath")
      .crumbs(ReportsBreadcrumbs.Attendance.Home(department, academicYear))
      .secondCrumbs(academicYearBreadcrumbs(academicYear)(urlGeneratorFactory(department)): _*)
  }

  @RequestMapping(method = Array(POST))
  def apply(
    @Valid @ModelAttribute("command") cmd: AllAttendanceReportCommand.CommandType,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    if (errors.hasErrors) Mav(new JSONErrorView(errors))
    else {
      val result = cmd.apply()
      val allStudents: Seq[Map[String, String]] = result.studentDataMap.keys.toSeq.sortBy(s => (s.lastName, s.firstName)).map(studentData =>
        Map(
          "firstName" -> studentData.firstName,
          "lastName" -> studentData.lastName,
          "userId" -> studentData.userId,
          "universityId" -> studentData.universityId,
          "yearOfStudy" -> studentData.yearOfStudy,
          "sprCode" -> studentData.sprCode,
          "route" -> studentData.routeCode,
          "tier4Requirements" -> studentData.tier4Requirements.toString
        )
      )
      val intervalFormatter = new IntervalFormatter
      val wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28)
      import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
      val allPoints: Seq[Map[String, String]] = result.studentDataMap.values.flatMap(_.keySet).toSeq.distinct.sortBy(p => (p.startDate, p.endDate)).map(point =>
        Map(
          "id" -> point.id,
          "name" -> point.name,
          "startDate" -> point.startDate.toDateTimeAtStartOfDay.getMillis.toString,
          "endDate" -> point.endDate.toDateTimeAtStartOfDay.getMillis.toString,
          "intervalString" -> intervalFormatter.execMethod(Seq(point.startDate, point.endDate)),
          "late" -> point.endDate.toDateTimeAtStartOfDay.plusDays(1).isBeforeNow.toString
        )
      )
      Mav(new JSONView(Map(
        "attendance" -> result.studentDataMap.map { case (studentData, pointMap) =>
          studentData.universityId -> pointMap.map { case (point, state) =>
            point.id -> Option(state).map(_.dbValue).orNull
          }
        },
        "students" -> allStudents,
        "points" -> allPoints,
        "reportRangeStartDate" -> ReportsDateFormats.ReportDate.print(result.reportRangeStartDate),
        "reportRangeEndDate" -> ReportsDateFormats.ReportDate.print(result.reportRangeEndDate)
      )))
    }
  }

  @RequestMapping(method = Array(POST), value = Array("/download.csv"))
  def csv(
    @ModelAttribute("processor") processor: AttendanceReportProcessor,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @RequestParam data: String
  ): CSVView = {
    val processorResult = getProcessorResult(processor, data)

    val writer = new StringWriter
    val csvBuilder = new AttendanceReportExporter(processorResult, department)
    val doc = new GoodCsvDocument(csvBuilder, null)

    doc.setHeaderLine(true)
    csvBuilder.headers foreach (header => doc.addHeaderField(header))
    processorResult.attendance.keys.foreach(item => doc.addLine(item))
    doc.write(writer)

    new CSVView(s"$filePrefix-${department.code}-${processorResult.reportRangeStartDate}-${processorResult.reportRangeEndDate}.csv", writer.toString)
  }

  @RequestMapping(method = Array(POST), value = Array("/download.xlsx"))
  def xlsx(
    @ModelAttribute("processor") processor: AttendanceReportProcessor,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @RequestParam data: String
  ): ExcelView = {
    val processorResult = getProcessorResult(processor, data)

    val workbook = new AttendanceReportExporter(processorResult, department).toXLSX

    new ExcelView(s"$filePrefix-${department.code}-${processorResult.reportRangeStartDate}-${processorResult.reportRangeEndDate}.xlsx", workbook)
  }

  @RequestMapping(method = Array(POST), value = Array("/download.xml"))
  def xml(
    @ModelAttribute("processor") processor: AttendanceReportProcessor,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    @RequestParam data: String
  ): Elem = {
    val processorResult = getProcessorResult(processor, data)

    new AttendanceReportExporter(processorResult, department).toXML
  }

  private def getProcessorResult(processor: AttendanceReportProcessor, data: String): AttendanceReportProcessorResult = {
    val request = JsonHelper.fromJson[AttendanceReportRequest](data)
    request.copyTo(processor)
    processor.apply()
  }

}

class AttendanceReportRequest extends Serializable {

  var attendance: JMap[String, JMap[String, String]] =
    LazyMaps.create { _: String => JMap[String, String]() }.asJava

  var students: JList[JMap[String, String]] = JArrayList()

  var points: JList[JMap[String, String]] = JArrayList()

  var reportRangeStartDate: String = _
  var reportRangeEndDate: String = _

  def copyTo(state: AttendanceReportProcessorState) {
    state.attendance = attendance

    state.students = students

    state.points = points

    state.reportRangeStartDate = ReportsDateFormats.ReportDate.parseDateTime(reportRangeStartDate).toLocalDate
    state.reportRangeEndDate = ReportsDateFormats.ReportDate.parseDateTime(reportRangeEndDate).toLocalDate
  }
}
