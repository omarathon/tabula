package uk.ac.warwick.tabula.reports.web.controllers.smallgroups

import java.io.StringWriter

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.commands.smallgroups._
import uk.ac.warwick.tabula.reports.web.ReportsBreadcrumbs
import uk.ac.warwick.tabula.reports.web.controllers.ReportsController
import uk.ac.warwick.tabula.web.views.{ExcelView, CSVView, JSONView}
import uk.ac.warwick.util.csv.GoodCsvDocument


@Controller
@RequestMapping(Array("/{department}/{academicYear}/groups/all"))
class AllSmallGroupsReportController extends ReportsController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AllSmallGroupsReportCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("processor")
	def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AllSmallGroupsReportProcessor(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET))
	def page(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav("smallgroups/allsmallgroups").crumbs(
			ReportsBreadcrumbs.Home.Department(department),
			ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
			ReportsBreadcrumbs.Attendance.Home(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def apply(
		@ModelAttribute("command") cmd: Appliable[AllSmallGroupsReportCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val result = cmd.apply()
		val allStudentsMap: Map[String, Map[String, String]] = result.students.map(studentUser => {
			studentUser.getWarwickId ->
				Map(
					"firstName" -> studentUser.getFirstName,
					"lastName" -> studentUser.getLastName,
					"userId" -> studentUser.getUserId
				)
		}).toMap
		val allEventsMap: Map[String, Map[String, String]] = result.eventWeeks.map(sgew => {
			sgew.id -> Map(
				"moduleCode" -> sgew.event.group.groupSet.module.code.toUpperCase,
				"setName" -> sgew.event.group.groupSet.nameWithoutModulePrefix,
				"format" -> sgew.event.group.groupSet.format.description,
				"groupName" -> sgew.event.group.name,
				"week" -> sgew.week.toString,
				"day" -> sgew.event.day.getAsInt.toString,
				"location" -> Option(sgew.event.location).map(_.toString).orNull,
				"tutors" -> sgew.event.tutors.users.map(u => s"${u.getFullName} (${u.getUserId})").mkString(", "),
				"eventId" -> sgew.event.id
			)
		}).toMap
		Mav(new JSONView(Map(
			"attendance" -> result.attendance.map{case(student, eventMap) =>
				student.getWarwickId -> eventMap.map{case(sgew, state) =>
					sgew.id -> Option(state).map(_.dbValue).orNull
				}
			}.toMap,
			"students" -> allStudentsMap,
			"events" -> allEventsMap
		)))
	}

	@RequestMapping(method = Array(POST), value = Array("/show"))
	def show(
		@ModelAttribute("processor") processor: Appliable[AllSmallGroupsReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()
		Mav("smallgroups/_allsmallgroups",
			"attendance" -> processorResult.attendance,
			"students" -> processorResult.students,
			"events" -> processorResult.events
		).noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.csv"))
	def csv(
		@ModelAttribute("processor") processor: Appliable[AllSmallGroupsReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		val writer = new StringWriter
		val csvBuilder = new AllSmallGroupsReportExporter(processorResult, department)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		processorResult.students.foreach(item => doc.addLine(item))
		doc.write(writer)

		new CSVView(s"all-small-group-attendance-${department.code}.csv", writer.toString)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xlsx"))
	def xlsx(
		@ModelAttribute("processor") processor: Appliable[AllSmallGroupsReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		val workbook = new AllSmallGroupsReportExporter(processorResult, department).toXLSX

		new ExcelView(s"all-small-group-attendance-${department.code}.xlsx", workbook)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xml"))
	def xml(
		@ModelAttribute("processor") processor: Appliable[AllSmallGroupsReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		new AllSmallGroupsReportExporter(processorResult, department).toXML
	}

}