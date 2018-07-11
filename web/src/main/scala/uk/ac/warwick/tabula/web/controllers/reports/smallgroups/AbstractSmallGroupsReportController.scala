package uk.ac.warwick.tabula.web.controllers.reports.smallgroups

import java.io.StringWriter
import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.reports.smallgroups._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentDataFetcher
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, JsonHelper}
import uk.ac.warwick.util.csv.GoodCsvDocument

import scala.collection.JavaConverters._
import scala.xml.Elem

abstract class AbstractSmallGroupsReportController extends ReportsController
	with DepartmentScopedController with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AttendanceMonitoringStudentDataFetcher {

	validatesSelf[SelfValidating]

	def command(department: Department, academicYear: AcademicYear): AllSmallGroupsReportCommand.CommandType

	val pageRenderPath: String
	val filePrefix: String
	def urlGeneratorFactory(department: Department): (AcademicYear) => String

	override val departmentPermission: Permission = Permissions.Department.Reports

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	type SmallGroupsReportProcessor = Appliable[SmallGroupsReportProcessorResult] with SmallGroupsReportProcessorState

	@ModelAttribute("processor")
	def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		SmallGroupsReportProcessor(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET))
	def page(
		@Valid @ModelAttribute("command") cmd: AllSmallGroupsReportCommand.CommandType,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav(s"reports/smallgroups/$pageRenderPath")
			.crumbs(ReportsBreadcrumbs.SmallGroups.Home(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(urlGeneratorFactory(department)): _*)
	}

	@RequestMapping(method = Array(POST))
	def apply(
		@Valid @ModelAttribute("command") cmd: AllSmallGroupsReportCommand.CommandType,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) Mav(new JSONErrorView(errors))
		else {
			val result = cmd.apply()
			val allStudents: Seq[Map[String, String]] = result.studentDatas.map(studentData =>
				Map(
					"universityId" -> studentData.universityId,
					"firstName" -> studentData.firstName,
					"lastName" -> studentData.lastName,
					"userId" -> studentData.userId,
					"yearOfStudy" -> studentData.yearOfStudy,
					"sprCode" -> studentData.sprCode,
					"route" -> studentData.routeCode,
					"tier4Requirements" -> studentData.tier4Requirements.toString
				)
			)
			val allEvents: Seq[Map[String, String]] = result.eventWeeks.map(sgew =>
				Map(
					"id" -> sgew.id,
					"moduleCode" -> sgew.event.group.groupSet.module.code.toUpperCase,
					"setName" -> sgew.event.group.groupSet.nameWithoutModulePrefix,
					"format" -> sgew.event.group.groupSet.format.description,
					"groupName" -> sgew.event.group.name,
					"week" -> sgew.week.toString,
					"day" -> sgew.event.day.getAsInt.toString,
					"dayString" -> sgew.event.day.shortName,
					"location" -> Option(sgew.event.location).map(_.toString).orNull,
					"tutors" -> sgew.event.tutors.users.map(u => s"${u.getFullName} (${u.getUserId})").mkString(", "),
					"eventId" -> sgew.event.id,
					"late" -> sgew.late.toString
				)
			)
			Mav(new JSONView(Map(
				"attendance" -> result.attendance.map { case (student, eventMap) =>
					student.getWarwickId -> eventMap.map { case (sgew, state) =>
						sgew.id -> Option(state).map(_.dbValue).orNull
					}
				},
				"students" -> allStudents,
				"events" -> allEvents
			)))
		}
	}

	@RequestMapping(method = Array(POST), value = Array("/download.csv"))
	def csv(
		@ModelAttribute("processor") processor: SmallGroupsReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): CSVView = {
		val processorResult = getProcessorResult(processor, data)

		val writer = new StringWriter
		val csvBuilder = new SmallGroupsReportExporter(processorResult, department)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		processorResult.students.foreach(item => doc.addLine(item))
		doc.write(writer)

		new CSVView(s"$filePrefix-${department.code}.csv", writer.toString)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xlsx"))
	def xlsx(
		@ModelAttribute("processor") processor: SmallGroupsReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): ExcelView = {
		val processorResult = getProcessorResult(processor, data)

		val workbook = new SmallGroupsReportExporter(processorResult, department).toXLSX

		new ExcelView(s"$filePrefix-${department.code}.xlsx", workbook)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xml"))
	def xml(
		@ModelAttribute("processor") processor: SmallGroupsReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): Elem = {
		val processorResult = getProcessorResult(processor, data)

		new SmallGroupsReportExporter(processorResult, department).toXML
	}

	private def getProcessorResult(processor: SmallGroupsReportProcessor, data: String): SmallGroupsReportProcessorResult = {
		val request = JsonHelper.fromJson[SmallGroupsReportRequest](data)
		request.copyTo(processor)
		processor.apply()
	}

}

class SmallGroupsReportRequest extends Serializable {

	var attendance: JMap[String, JMap[String, String]] =
		LazyMaps.create{_: String => JMap[String, String]() }.asJava

	var students: JList[JMap[String, String]] = JArrayList()

	var events: JList[JMap[String, String]] = JArrayList()

	def copyTo(state: SmallGroupsReportProcessorState) {
		state.attendance = attendance

		state.students = students

		state.events = events
	}
}