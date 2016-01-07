package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns

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

}

trait HasExamGridColumnCategory {

	self: ExamGridColumn =>

	val category: String

}

trait HasExamGridColumnSecondaryValue {

	self: ExamGridColumn =>

	val renderSecondaryValue: String

}

trait HasExamGridColumnSection {

	self: ExamGridColumn =>

	val sectionIdentifier: String

	val sectionTitleLabel: String

	val sectionSecondaryValueLabel: String

	val sectionValueLabel: String
}

object BlankColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "blank"

	override val sortOrder: Int = Int.MaxValue

	case class Column(scyds: Seq[StudentCourseYearDetails], override val title: String)
		extends ExamGridColumn(scyds) with HasExamGridColumnCategory {

		override val category: String = "Additional"

		override def render: Map[String, String] =
			scyds.map(scyd => scyd.id -> "").toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			scyd: StudentCourseYearDetails,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			row.createCell(index)
		}

	}

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] = throw new UnsupportedOperationException

	def getColumn(title: String): Seq[ExamGridColumn] = Seq(Column(Nil, title))

}
