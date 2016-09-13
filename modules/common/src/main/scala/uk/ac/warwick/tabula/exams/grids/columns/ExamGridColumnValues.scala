package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFCellStyle}
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.helpers.StringUtils._

sealed trait ExamGridColumnValue {
	def toHTML: String
	def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit
}

object ExamGridColumnValueDecimal {
	def apply(value: BigDecimal, isActual: Boolean = false) = new ExamGridColumnValueDecimal(value, isActual)
}
class ExamGridColumnValueDecimal(value: BigDecimal, isActual: Boolean = false) extends ExamGridColumnValue {
	override def toHTML: String = if (isActual) "<span class=\"exam-grid-actual-mark\">%s</span>".format(value.underlying.stripTrailingZeros().toPlainString) else value.underlying.stripTrailingZeros().toPlainString
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(value.underlying.stripTrailingZeros().doubleValue)
		if (isActual) {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.ActualMark))
		}
	}
}

object ExamGridColumnValueString {
	def apply(value: String) = new ExamGridColumnValueString(value)
}
class ExamGridColumnValueString(val value: String) extends ExamGridColumnValue {
	override def toHTML: String = value
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(value)
	}
}

case class ExamGridColumnValueFailed(value: BigDecimal, isActual: Boolean = false) extends ExamGridColumnValueDecimal(value) {
	override def toHTML: String = "<span class=\"exam-grid-fail %s\">%s</span>".format(
		if (isActual) "exam-grid-actual-mark" else "",
		value.underlying.stripTrailingZeros().toPlainString
	)
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(value.underlying.stripTrailingZeros().doubleValue)
		if (isActual) {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.FailAndActualMark))
		} else {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Fail))
		}

	}
}

case class ExamGridColumnValueOvercat(value: BigDecimal, isActual: Boolean = false) extends ExamGridColumnValueDecimal(value) {
	override def toHTML: String = "<span class=\"exam-grid-overcat\">%s</span>".format(value.underlying.stripTrailingZeros().toPlainString)
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(value.underlying.stripTrailingZeros().doubleValue)
		if (isActual) {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.OvercatAndActualMark))
		} else {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Overcat))
		}
	}
}

case class ExamGridColumnValueOverride(value: BigDecimal) extends ExamGridColumnValueDecimal(value) {
	override def toHTML: String = "<span class=\"exam-grid-override\">%s</span>".format(value.underlying.stripTrailingZeros().toPlainString)
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(value.underlying.stripTrailingZeros().doubleValue)
		cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Overridden))
	}
}

case class ExamGridColumnValueMissing(message: String = "") extends ExamGridColumnValueString("X") {
	override def toHTML: String = "<span class=\"exam-grid-actual-mark %s\" %s>X</span>".format(
		if (message.hasText) "use-tooltip" else "",
		if (message.hasText) "title=\"%s\" data-container=\"body\"".format(message) else ""
	)
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(value)
		cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.ActualMark))
	}
}

case class ExamGridColumnValueStringHtmlOnly(override val value: String) extends ExamGridColumnValueString(value) {
	override def toHTML: String = super.toHTML
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {}
}
