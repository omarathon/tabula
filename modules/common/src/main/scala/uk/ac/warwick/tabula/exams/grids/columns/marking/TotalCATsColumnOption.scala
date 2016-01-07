package uk.ac.warwick.tabula.exams.grids.columns.marking

import java.math

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}

@Component
class TotalCATsColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "cats"

	override val sortOrder: Int = 4

	case class Column(scyds: Seq[StudentCourseYearDetails], bound: BigDecimal, isUpperBound: Boolean = false, isTotal: Boolean = false)
		extends ExamGridColumn(scyds) with HasExamGridColumnCategory {

		override val title: String = if (isTotal) "Total Cats" else if (isUpperBound) s"<=$bound" else s">=$bound"

		override val category: String = "CATS"

		override def render: Map[String, String] =
			scyds.map(scyd => scyd.id -> renderValue(scyd).map(_.toPlainString).getOrElse("")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			scyd: StudentCourseYearDetails,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			renderValue(scyd).foreach(bd => cell.setCellValue(bd.doubleValue()))
		}

		private def renderValue(scyd: StudentCourseYearDetails): Option[math.BigDecimal] = {
			val filteredRegistrations =
				if (isTotal) {
					scyd.moduleRegistrations
				} else if (isUpperBound) {
					scyd.moduleRegistrations.filter(mr => Option(mr.agreedMark).exists(mark => BigDecimal(mark) <= bound))
				} else {
					scyd.moduleRegistrations.filter(mr => Option(mr.agreedMark).exists(mark => BigDecimal(mark) >= bound))
				}

			if (filteredRegistrations.nonEmpty)
				Some(filteredRegistrations.map(mr => BigDecimal(mr.cats)).sum.underlying.stripTrailingZeros())
			else
				None
		}

	}

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] =
		Seq(
			Column(scyds, new math.BigDecimal(40), isUpperBound = true),
			Column(scyds, new math.BigDecimal(40)),
			Column(scyds, new math.BigDecimal(50)),
			Column(scyds, new math.BigDecimal(60)),
			Column(scyds, new math.BigDecimal(70)),
			Column(scyds, new math.BigDecimal(0), isTotal = true)
		)

}
