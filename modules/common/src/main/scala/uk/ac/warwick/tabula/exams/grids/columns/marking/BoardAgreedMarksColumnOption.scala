package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}

@Component
class BoardAgreedMarksColumnOption extends YearColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "board"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.BoardAgreedMark

	override val mandatory = true

	case class Column(entities: Seq[GenerateExamGridEntity], yearOfStudy: Int) extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = "Board Agreed Mark"

		override val category: String = s"Year $yearOfStudy Marks"

		override def render: Map[String, String] =
			entities.map(entity => entity.id ->
				entity.studentCourseYearDetails.flatMap(scyd => Option(scyd.agreedMark)).map(_.toPlainString).getOrElse("")
			).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			entity.studentCourseYearDetails.flatMap(scyd => Option(scyd.agreedMark)).foreach(mark => {
				cell.setCellValue(mark.doubleValue)
			})
		}

	}

	def getColumns(yearOfStudy: Int, entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		Seq(Column(entities, yearOfStudy))

}
