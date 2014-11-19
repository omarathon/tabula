package uk.ac.warwick.tabula.reports.web.controllers.attendancemonitoring

import java.io.StringWriter

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.reports.commands.attendancemonitoring._
import uk.ac.warwick.tabula.reports.web.ReportsBreadcrumbs
import uk.ac.warwick.tabula.reports.web.controllers.ReportsController
import uk.ac.warwick.tabula.web.views.{CSVView, JSONView}
import uk.ac.warwick.util.csv.GoodCsvDocument


@Controller
@RequestMapping(Array("/{department}/{academicYear}/attendance/all"))
class AllAttendanceReportController extends ReportsController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AllAttendanceReportCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("processor")
	def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AllAttendanceReportProcessor(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET))
	def page(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav("attendancemonitoring/allattendance").crumbs(
			ReportsBreadcrumbs.Home.Department(department),
			ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
			ReportsBreadcrumbs.Attendance.Home(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def apply(
		@ModelAttribute("command") cmd: Appliable[Map[AttendanceMonitoringStudentData, Map[AttendanceMonitoringPoint, AttendanceState]]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val result = cmd.apply()
		val allStudentsMap: Map[String, Map[String, String]] = result.keys.toSeq.sortBy(s => (s.lastName, s.firstName)).map(studentData => {
			studentData.universityId ->
				Map(
					"firstName" -> studentData.firstName,
					"lastName" -> studentData.lastName,
					"userId" -> studentData.userId
				)
		}).toMap
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		val allPointsMap: Map[String, Map[String, String]] = result.values.flatMap(_.keySet).toSeq.sortBy(p => (p.startDate, p.endDate)).map(point => {
			point.id -> Map(
				"name" -> point.name,
				"startDate" -> point.startDate.toDateTimeAtStartOfDay.getMillis.toString,
				"endDate" -> point.endDate.toDateTimeAtStartOfDay.getMillis.toString
			)
		}).toMap
		Mav(new JSONView(Map(
			"result" -> result.map{case(studentData, pointMap) =>
				studentData.universityId -> pointMap.map{case(point, state) =>
					point.id -> Option(state).map(_.dbValue).orNull
				}
			}.toMap,
			"students" -> allStudentsMap,
			"points" -> allPointsMap
		)))
	}

	@RequestMapping(method = Array(POST), value = Array("/show"))
	def show(
		@ModelAttribute("processor") processor: Appliable[AllAttendanceReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()
		Mav("attendancemonitoring/_allattendance",
			"result" -> processorResult.result,
			"students" -> processorResult.students,
			"points" -> processorResult.points
		).noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST), value = Array("/download"))
	def download(
		@ModelAttribute("processor") processor: Appliable[AllAttendanceReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		val writer = new StringWriter
		val csvBuilder = new AllAttendanceReportExporter(processorResult)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		processorResult.result.keys.foreach(item => doc.addLine(item))
		doc.write(writer)

		new CSVView(s"all-attendance-${department.code}.csv", writer.toString)
	}

}