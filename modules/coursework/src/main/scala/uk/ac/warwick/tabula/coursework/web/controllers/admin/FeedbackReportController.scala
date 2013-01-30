package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Features
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions._
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.commands.departments.FeedbackReportCommand
import org.apache.poi.xssf.usermodel.{ XSSFSheet, XSSFWorkbook }
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.ss.usermodel.{ IndexedColors, ComparisonOperator }
import org.apache.poi.ss.util.CellRangeAddress
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.model.Submission
import java.util.ArrayList
import org.joda.time.format.DateTimeFormat
import org.apache.poi.xssf.usermodel.XSSFFont
import uk.ac.warwick.tabula.data.model.AuditEvent

@Controller
@RequestMapping(Array("/admin/department/{dept}/reports/feedback"))
class FeedbackReportController extends CourseworkController {
	
	@ModelAttribute def command(@PathVariable(value = "dept") dept: Department) =
		new FeedbackReportCommand(dept)

	@RequestMapping(method = Array(HEAD, GET))
	def generateReport(cmd: FeedbackReportCommand) = {
		new ExcelView(cmd.department.getName + " feedback report.xlsx", cmd.apply())
	}
		
}