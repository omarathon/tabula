package uk.ac.warwick.tabula.reports.web.controllers.smallgroups

import java.io.StringWriter

import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.commands.smallgroups._
import uk.ac.warwick.tabula.reports.web.controllers.ReportsController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, JSONView}
import uk.ac.warwick.util.csv.GoodCsvDocument


abstract class AbstractSmallGroupsByModuleReportController extends ReportsController {

	def filteredAttendanceCommand(department: Department, academicYear: AcademicYear): Appliable[AllSmallGroupsReportCommandResult]

	val filePrefix: String

	def page(department: Department, academicYear: AcademicYear): Mav

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		SmallGroupsByModuleReportCommand(department, academicYear)

	@ModelAttribute("processor")
	def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		SmallGroupsByModuleReportProcessor(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(POST))
	def apply(
		@ModelAttribute("command") cmd: Appliable[SmallGroupsByModuleReportCommandResult] with SetsFilteredAttendance,
		@ModelAttribute("filteredAttendanceCommand") filteredAttendanceCmd: Appliable[AllSmallGroupsReportCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		cmd.setFilteredAttendance(filteredAttendanceCmd.apply())
		val result = cmd.apply()
		val allStudents: Seq[Map[String, String]] = result.students.map(studentUser =>
			Map(
				"firstName" -> studentUser.getFirstName,
				"lastName" -> studentUser.getLastName,
				"userId" -> studentUser.getUserId,
				"universityId" -> studentUser.getWarwickId
			)
		)
		val allModules: Seq[Map[String, String]] = result.modules.map(module =>
			Map(
				"id" -> module.id,
				"code" -> module.code,
				"name" -> module.name
			)
		)
		Mav(new JSONView(Map(
			"counts" -> result.counts.map{case(student, moduleMap) =>
				student.getWarwickId -> moduleMap.map{case(module, count) =>
					module.id -> count.toString
				}
			}.toMap,
			"students" -> allStudents,
			"modules" -> allModules
		)))
	}

	@RequestMapping(method = Array(POST), value = Array("/download.csv"))
	def csv(
		@ModelAttribute("processor") processor: Appliable[SmallGroupsByModuleReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		val writer = new StringWriter
		val csvBuilder = new SmallGroupsByModuleReportExporter(processorResult, department)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		processorResult.students.foreach(item => doc.addLine(item))
		doc.write(writer)

		new CSVView(s"$filePrefix-${department.code}.csv", writer.toString)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xlsx"))
	def xlsx(
		@ModelAttribute("processor") processor: Appliable[SmallGroupsByModuleReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		val workbook = new SmallGroupsByModuleReportExporter(processorResult, department).toXLSX

		new ExcelView(s"$filePrefix-${department.code}.xlsx", workbook)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xml"))
	def xml(
		@ModelAttribute("processor") processor: Appliable[SmallGroupsByModuleReportProcessorResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val processorResult = processor.apply()

		new SmallGroupsByModuleReportExporter(processorResult, department).toXML
	}

}