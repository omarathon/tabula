package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, ExamGridColumnState, HasExamGridColumnCategory}

@Component
class TotalCATsColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "cats"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.TotalCATs

	case class Column(state: ExamGridColumnState, bound: BigDecimal, isUpperBound: Boolean = false, isTotal: Boolean = false)
		extends ExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = if (isTotal) "Total Cats" else if (isUpperBound) s"<=$bound" else s">=$bound"

		override val category: String = "CATS"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> renderValue(entity).map(_.toPlainString).getOrElse("")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			renderValue(entity).foreach(bd => cell.setCellValue(bd.doubleValue()))
		}

		private def renderValue(entity: GenerateExamGridEntity): Option[JBigDecimal] = {
			val filteredRegistrations =
				if (isTotal) {
					entity.moduleRegistrations
				} else if (isUpperBound) {
					entity.moduleRegistrations.filter(mr => mr.firstDefinedMark.exists(mark => BigDecimal(mark) <= bound))
				} else {
					entity.moduleRegistrations.filter(mr => mr.firstDefinedMark.exists(mark => BigDecimal(mark) >= bound))
				}

			if (filteredRegistrations.nonEmpty)
				Some(filteredRegistrations.map(mr => BigDecimal(mr.cats)).sum.underlying.stripTrailingZeros())
			else
				None
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] =
		Seq(
			Column(state, BigDecimal(30), isUpperBound = true),
			Column(state, BigDecimal(40)),
			Column(state, BigDecimal(50)),
			Column(state, BigDecimal(60)),
			Column(state, BigDecimal(70)),
			Column(state, BigDecimal(0), isTotal = true)
		)

}
