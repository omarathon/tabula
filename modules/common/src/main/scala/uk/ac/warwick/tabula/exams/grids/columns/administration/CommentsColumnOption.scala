package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}

@Component
class CommentsColumnOption extends ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "comments"

	override val sortOrder: Int = 20

	case class Column(entities: Seq[GenerateExamGridEntity]) extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = "Comments"

		override val category: String = "Administration"

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> "").toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			row.createCell(index)
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(Column(entities))

}
