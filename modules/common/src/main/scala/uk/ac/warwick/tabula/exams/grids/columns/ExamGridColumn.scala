package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFCell, XSSFCellStyle}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails

object ExamGridColumnOption {
	type Identifier = String
	implicit val defaultOrdering = Ordering.by { columnOption: ExamGridColumnOption => columnOption.sortOrder }
}

@Component
trait ExamGridColumnOption {

	val identifier: ExamGridColumnOption.Identifier
	val sortOrder: Int
	def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn]

}

abstract class ExamGridColumn(scyds: Seq[StudentCourseYearDetails]) {

	val title: String
	def render: Map[String, String]
	def renderExcelCell(
		row: XSSFRow,
		index: Int,
		scyd: StudentCourseYearDetails,
		cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
	): Unit

	protected def createCell(row: XSSFRow): XSSFCell = row.createCell(Math.max(row.getLastCellNum, 0))

}

trait HasExamGridColumnCategory {

	self: ExamGridColumn =>

	def category: String

}

trait HasExamGridColumnSecondaryValue {

	self: ExamGridColumn =>

	def secondaryValueTitle: String
	def renderSecondaryValue: String

}
