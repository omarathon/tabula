package uk.ac.warwick.tabula.exams.web.controllers.admin

import java.io.StringWriter

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionAndFeedbackCommand.Student
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.{ViewExamCommand, ViewExamCommandResult}
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView}
import uk.ac.warwick.util.csv.GoodCsvDocument

import scala.collection.mutable

@Controller
@RequestMapping(Array("/admin/module/{module}/{academicYear}/exams/{exam}"))
class ExportExamsController extends ExamsController with ExamExports with requestData {

	@RequestMapping(Array("/export.csv"))
	def csv (
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear
	) = {

		val (results, students) = getData(module,exam,academicYear)
		val writer = new StringWriter
		val csvBuilder = new CSVBuilder(students, results, exam, module, academicYear)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))

		students foreach (item => doc.addLine(item))
		doc.write(writer)

		new CSVView(module.code + "-" + exam.name + ".csv", writer.toString)
	}

	@RequestMapping(Array("/export.xml"))
	def xml(
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear
	) = {

		val (results, students) = getData(module,exam,academicYear)
		new XMLBuilder(students, results, exam, module, academicYear).toXML
	}

	@RequestMapping(Array("/export.xlsx"))
	def toXLSX(
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear
	) = {

		val (results, students) = getData(module,exam,academicYear)
		val workbook = new ExcelBuilder(students, results, module).toXLSX
		new ExcelView(module.code + "-" + exam.name + ".xlsx", workbook)
	}
}

trait requestData {

	def getData(module: Module, exam: Exam, academicYear: AcademicYear) : (ViewExamCommandResult, mutable.MutableList[Student]) = {

		val command = ViewExamCommand(module, academicYear, exam)
		val results = command.apply();

		val students = mutable.MutableList[Student]()
		for (user <- results.students) {
			students += new Student(user, null, null, null, null, null)
		}
		(results, students)
	}
}