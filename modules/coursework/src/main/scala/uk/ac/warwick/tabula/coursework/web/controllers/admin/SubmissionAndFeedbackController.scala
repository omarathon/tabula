package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.AuditEventIndexService
import java.io.StringWriter
import uk.ac.warwick.util.csv.GoodCsvDocument
import uk.ac.warwick.tabula.web.views.CSVView
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.csv.CSVLineWriter
import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.DateFormats
import org.joda.time.ReadableInstant
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.xml._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.commons.lang3.text.WordUtils
import uk.ac.warwick.tabula.web.views.ExcelView
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.coursework.helpers.{CourseworkFilter, CourseworkFilters}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.Features
import javax.validation.Valid
import org.springframework.validation.Errors

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}"))
class SubmissionAndFeedbackController extends CourseworkController {

	var auditIndexService = Wire.auto[AuditEventIndexService]
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]
	var features = Wire.auto[Features]
	
	validatesSelf[SubmissionAndFeedbackCommand]
	
	@ModelAttribute("assignment") 
	def assignment(@PathVariable("assignment") assignment: Assignment) = assignment
	
	@ModelAttribute("submissionAndFeedbackCommand")
	def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) = 
		new SubmissionAndFeedbackCommand(module, assignment)
	
	@ModelAttribute("allFilters") 
	def allFilters(@PathVariable("assignment") assignment: Assignment) =
		CourseworkFilters.AllFilters.filter(_.applies(assignment))

	@RequestMapping(Array("/list"))
	def list(@Valid command: SubmissionAndFeedbackCommand, errors: Errors) = {
		val (assignment, module) = (command.assignment, command.module)
		
		if (!features.assignmentProgressTable) Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
		else {
			if (errors.hasErrors) {
				Mav("admin/assignments/submissionsandfeedback/progress")
					.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
			} else {
				val results = command.apply()
				
				Mav("admin/assignments/submissionsandfeedback/progress",
					"students" -> results.students,
					"whoDownloaded" -> results.whoDownloaded,
					"stillToDownload" -> results.stillToDownload,
					"hasPublishedFeedback" -> results.hasPublishedFeedback,
					"hasOriginalityReport" -> results.hasOriginalityReport,
					"mustReleaseForMarking" -> results.mustReleaseForMarking
				).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
			}
		}
	}
	
	@RequestMapping(Array("/table"))
	def table(@Valid command: SubmissionAndFeedbackCommand, errors: Errors) = {
		val (assignment, module) = (command.assignment, command.module)

		if (errors.hasErrors) {
			Mav("admin/assignments/submissionsandfeedback/list")
				.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
		} else {
			val results = command.apply()
		
			Mav("admin/assignments/submissionsandfeedback/list",
				"students" -> results.students,
				"whoDownloaded" -> results.whoDownloaded,
				"stillToDownload" -> results.stillToDownload,
				"hasPublishedFeedback" -> results.hasPublishedFeedback,
				"hasOriginalityReport" -> results.hasOriginalityReport,
				"mustReleaseForMarking" -> results.mustReleaseForMarking
			).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
		}
	}
	
	@RequestMapping(Array("/export.csv"))
	def csv(@Valid command: SubmissionAndFeedbackCommand) = {
		val (assignment, module) = (command.assignment, command.module)
		val results = command.apply()
		
		val items = results.students

		val writer = new StringWriter
		val csvBuilder = new CSVBuilder(items, assignment, module)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		items foreach (item => doc.addLine(item))
		doc.write(writer)

		new CSVView(module.code + "-" + assignment.id + ".csv", writer.toString)
	}
	
	@RequestMapping(Array("/export.xml"))
	def xml(@Valid command: SubmissionAndFeedbackCommand) = {
		val (assignment, module) = (command.assignment, command.module)
		val results = command.apply()
		
		val items = results.students
		
		new XMLBuilder(items, assignment, module).toXML
	}
	
	@RequestMapping(Array("/export.xlsx"))
	def xlsx(@Valid command: SubmissionAndFeedbackCommand) = {
		val (assignment, module) = (command.assignment, command.module)
		val results = command.apply()
		
		val items = results.students
		
		val workbook = new ExcelBuilder(items, assignment, module).toXLSX
		
		new ExcelView(assignment.name + ".xlsx", workbook)
	}
	
	override def binding[SubmissionAndFeedbackCommand](binder: WebDataBinder, cmd: SubmissionAndFeedbackCommand) {
		binder.registerCustomEditor(classOf[CourseworkFilter], new AbstractPropertyEditor[CourseworkFilter] {
			override def fromString(name: String) = CourseworkFilters.of(name)			
			override def toString(filter: CourseworkFilter) = filter.getName
		})
	}
	
}