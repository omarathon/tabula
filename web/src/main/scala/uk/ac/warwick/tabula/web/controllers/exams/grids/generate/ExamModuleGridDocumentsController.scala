package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import org.springframework.web.servlet.View
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.ExcelView

trait ExamModuleGridDocumentsController extends ExamsController {

	self: GenerateModuleExamGridController =>

	var messageSource: MessageSource = Wire.auto[MessageSource]

	@RequestMapping(method = Array(POST), params = Array(GenerateModuleExamGridMappingParameters.excel))
	def excel(
		@Valid @ModelAttribute("selectModuleExamCommand") selectModuleExamCommand: SelectModuleExamCommand,
		selectModuleExamCommandErrors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): View = {
		if (selectModuleExamCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}

		val workbook =
			GenerateModuleExamGridExporter(
				department = department,
				academicYear = academicYear,
				module = selectModuleExamCommand.module,
				examModuleGridResult = selectModuleExamCommand.apply()
			)


		new ExcelView(
			s"Exam grid for ${department.name} ${selectModuleExamCommand.module.code} ${academicYear.toString.replace("/", "-")}.xlsx",
			workbook
		)
	}

}
