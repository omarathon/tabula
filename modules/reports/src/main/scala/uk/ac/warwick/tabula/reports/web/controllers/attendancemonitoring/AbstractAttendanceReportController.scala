package uk.ac.warwick.tabula.reports.web.controllers.attendancemonitoring

import java.io.StringWriter

import freemarker.template.{Configuration, DefaultObjectWrapper}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.IntervalFormatter
import uk.ac.warwick.tabula.reports.commands.attendancemonitoring.AllAttendanceReportCommand.AllAttendanceReportCommandResult
import uk.ac.warwick.tabula.reports.commands.attendancemonitoring._
import uk.ac.warwick.tabula.reports.web.ReportsBreadcrumbs
import uk.ac.warwick.tabula.reports.web.controllers.ReportsController
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, JSONView}
import uk.ac.warwick.util.csv.GoodCsvDocument
import uk.ac.warwick.tabula.JavaImports._


abstract class AbstractAttendanceReportController extends ReportsController {

	def command(department: Department, academicYear: AcademicYear): Appliable[AllAttendanceReportCommandResult]

	val pageRenderPath: String
	val filePrefix: String

	@ModelAttribute("processor")
	def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AttendanceReportProcessor(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET))
	def page(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav(s"attendancemonitoring/$pageRenderPath").crumbs(
			ReportsBreadcrumbs.Home.Department(department),
			ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
			ReportsBreadcrumbs.Attendance.Home(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def apply(
		@ModelAttribute("command") cmd: Appliable[AllAttendanceReportCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val result = cmd.apply()
		val allStudents: Seq[Map[String, String]] = result.keys.toSeq.sortBy(s => (s.lastName, s.firstName)).map(studentData =>
			 Map(
					"firstName" -> studentData.firstName,
					"lastName" -> studentData.lastName,
					"userId" -> studentData.userId,
					"universityId" -> studentData.universityId
				)
		)
		val intervalFormatter = new IntervalFormatter
		val wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_0)
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		val allPoints: Seq[Map[String, String]] = result.values.flatMap(_.keySet).toSeq.distinct.sortBy(p => (p.startDate, p.endDate)).map(point =>
			Map(
				"id" -> point.id,
				"name" -> point.name,
				"startDate" -> point.startDate.toDateTimeAtStartOfDay.getMillis.toString,
				"endDate" -> point.endDate.toDateTimeAtStartOfDay.getMillis.toString,
				"intervalString" -> intervalFormatter.exec(JList(wrapper.wrap(point.startDate), wrapper.wrap(point.endDate))).asInstanceOf[String],
				"late" -> point.endDate.toDateTimeAtStartOfDay.plusDays(1).isBeforeNow.toString
			)
		)
		Mav(new JSONView(Map(
			"attendance" -> result.map{case(studentData, pointMap) =>
				studentData.universityId -> pointMap.map{case(point, state) =>
					point.id -> Option(state).map(_.dbValue).orNull
				}
			}.toMap,
			"students" -> allStudents,
			"points" -> allPoints
		)))
	}

	@RequestMapping(method = Array(POST), value = Array("/download.csv"))
	def csv(
		@ModelAttribute("processor") processor: Appliable[AttendanceReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		val writer = new StringWriter
		val csvBuilder = new AttendanceReportExporter(processorResult, department)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		processorResult.attendance.keys.foreach(item => doc.addLine(item))
		doc.write(writer)

		new CSVView(s"$filePrefix-${department.code}.csv", writer.toString)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xlsx"))
	def xlsx(
		@ModelAttribute("processor") processor: Appliable[AttendanceReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		val workbook = new AttendanceReportExporter(processorResult, department).toXLSX

		new ExcelView(s"$filePrefix-${department.code}.xlsx", workbook)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xml"))
	def xml(
		@ModelAttribute("processor") processor: Appliable[AttendanceReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		new AttendanceReportExporter(processorResult, department).toXML
	}

}
