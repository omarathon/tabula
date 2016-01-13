package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns.{HasExamGridColumnCategory, ExamGridColumn, ExamGridColumnOption}

@Component
class CommentsColumnOption extends ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "comments"

	override val sortOrder: Int = 20

	case class Column(scyds: Seq[StudentCourseYearDetails]) extends ExamGridColumn(scyds) with HasExamGridColumnCategory {

		override val title: String = "Comments"

		override val category: String = "Administration"

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

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] = Seq(Column(scyds))

}
