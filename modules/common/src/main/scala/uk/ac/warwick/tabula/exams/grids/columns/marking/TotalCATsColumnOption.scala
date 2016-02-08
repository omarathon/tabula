package uk.ac.warwick.tabula.exams.grids.columns.marking

import java.math

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}

@Component
class TotalCATsColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "cats"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.TotalCATs

	case class Column(entities: Seq[GenerateExamGridEntity], bound: BigDecimal, isUpperBound: Boolean = false, isTotal: Boolean = false)
		extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = if (isTotal) "Total Cats" else if (isUpperBound) s"<=$bound" else s">=$bound"

		override val category: String = "CATS"

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> renderValue(entity).map(_.toPlainString).getOrElse("")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			renderValue(entity).foreach(bd => cell.setCellValue(bd.doubleValue()))
		}

		private def renderValue(entity: GenerateExamGridEntity): Option[math.BigDecimal] = {
			val filteredRegistrations =
				if (isTotal) {
					entity.moduleRegistrations
				} else if (isUpperBound) {
					entity.moduleRegistrations.filter(mr => Option(mr.agreedMark).exists(mark => BigDecimal(mark) <= bound))
				} else {
					entity.moduleRegistrations.filter(mr => Option(mr.agreedMark).exists(mark => BigDecimal(mark) >= bound))
				}

			if (filteredRegistrations.nonEmpty)
				Some(filteredRegistrations.map(mr => BigDecimal(mr.cats)).sum.underlying.stripTrailingZeros())
			else
				None
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		Seq(
			Column(entities, new math.BigDecimal(30), isUpperBound = true),
			Column(entities, new math.BigDecimal(40)),
			Column(entities, new math.BigDecimal(50)),
			Column(entities, new math.BigDecimal(60)),
			Column(entities, new math.BigDecimal(70)),
			Column(entities, new math.BigDecimal(0), isTotal = true)
		)

}
