package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.poi.ss.usermodel.{Cell, CellStyle}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridExportStyles, GenerateExamGridExporter}
import uk.ac.warwick.tabula.helpers.StringUtils._

sealed abstract class ExamGridColumnValueType(val label: String, val description: String)

object ExamGridColumnValueType {
	case object Overall extends ExamGridColumnValueType("O", "Overall")
	case object Assignment extends ExamGridColumnValueType("A", "Assignment")
	case object Exam extends ExamGridColumnValueType("E", "Exam")

	def toMap(overall: ExamGridColumnValue, assignments: Seq[ExamGridColumnValue], exams: Seq[ExamGridColumnValue]): Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]] =
		Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]](
			Overall -> Seq(overall),
			Assignment -> assignments,
			Exam -> exams
		)

	def toMap(overall: ExamGridColumnValue): Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]] =
		Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]](
			Overall -> Seq(overall),
			Assignment -> Seq(ExamGridColumnValueString("")),
			Exam -> Seq(ExamGridColumnValueString(""))
		)
}

sealed trait ExamGridColumnValue {
	protected def getValueStringForRender: String
	protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit
	val isActual: Boolean
	def toHTML: String
	def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit
	def isEmpty: Boolean
}

object ExamGridColumnValue {
	def merge(values: Seq[ExamGridColumnValue]): ExamGridColumnValue = {
		values match {
			case _ if values.isEmpty => ExamGridColumnValueString("")
			case _ if values.size == 1 => values.head
			case _ =>
				// First see if they're ALL actual marks or not
				if (values.tail.forall(_.isActual == values.head.isActual)) {
					// If they're ALL actual or not, check other cell styles
					values.head match {
						case _: ExamGridColumnValueFailed if values.tail.forall(_.isInstanceOf[ExamGridColumnValueFailed]) =>
							ExamGridColumnValueFailedString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
						case _: ExamGridColumnValueOvercat if values.tail.forall(_.isInstanceOf[ExamGridColumnValueOvercat]) =>
							ExamGridColumnValueOvercatString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
						case _: ExamGridColumnValueOverride if values.tail.forall(_.isInstanceOf[ExamGridColumnValueOverride]) =>
							ExamGridColumnValueOverrideString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
						case _: ExamGridColumnValueMissing if values.tail.forall(_.isInstanceOf[ExamGridColumnValueMissing]) =>
							ExamGridColumnValueMissing()
						case _ =>
							ExamGridColumnValueString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
					}
				} else {
					// If only some are actual we can't apply a common style, so just return a plain merged string
					ExamGridColumnValueString(values.map(_.getValueStringForRender).mkString(","))
				}
		}
	}
}

object ExamGridColumnValueDecimal {
	def apply(value: BigDecimal, isActual: Boolean = false) = new ExamGridColumnValueDecimal(value, isActual)
}
class ExamGridColumnValueDecimal(value: BigDecimal, val isActual: Boolean = false) extends ExamGridColumnValue {
	protected final def getValueForRender: JBigDecimal = value.underlying.stripTrailingZeros()
	override protected final def getValueStringForRender: String = getValueForRender.toPlainString
	override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(ExamGridExportStyles.ActualMark))
		}
	}
	override def toHTML: String =
		if (isActual) "<span class=\"exam-grid-actual-mark\">%s</span>".format(getValueStringForRender)
		else getValueStringForRender
	override final def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		cell.setCellValue(getValueForRender.doubleValue)
		applyCellStyle(cell, cellStyleMap)
	}

	override def isEmpty: Boolean = value == null
}

object ExamGridColumnValueString {
	def apply(value: String, isActual: Boolean = false) = new ExamGridColumnValueString(value, isActual)
}
class ExamGridColumnValueString(value: String, val isActual: Boolean = false) extends ExamGridColumnValue {
	override protected final def getValueStringForRender: String = value
	override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(ExamGridExportStyles.ActualMark))
		}
	}
	override def toHTML: String =
		if (isActual) "<span class=\"exam-grid-actual-mark\">%s</span>".format(value)
		else value
	override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		cell.setCellValue(value)
		applyCellStyle(cell, cellStyleMap)
	}

	override def isEmpty: Boolean = !value.hasText
}

case class ExamGridColumnValueWithTooltip(value:String, actual:Boolean, message: String = "") extends ExamGridColumnValueString(value, actual){
	override def toHTML: String = "<span class=\"use-tooltip\" %s>%s</span>".format(
		if (message.hasText) "title=\"%s\" data-container=\"body\"".format(message) else "",
		value
	)
}

trait ExamGridColumnValueFailed {

	self: ExamGridColumnValue =>

	override def toHTML: String = "<span class=\"exam-grid-fail %s\">%s</span>".format(
		if (isActual) "exam-grid-actual-mark" else "",
		getValueStringForRender
	)

	override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(ExamGridExportStyles.FailAndActualMark))
		} else {
			cell.setCellStyle(cellStyleMap(ExamGridExportStyles.Fail))
		}
	}
}

case class ExamGridColumnValueFailedDecimal(value: BigDecimal, override val isActual: Boolean = false)
	extends ExamGridColumnValueDecimal(value) with ExamGridColumnValueFailed

case class ExamGridColumnValueFailedString(value: String, override val isActual: Boolean = false)
	extends ExamGridColumnValueString(value) with ExamGridColumnValueFailed


trait ExamGridColumnValueOvercat {

	self: ExamGridColumnValue =>

	override def toHTML: String = "<span class=\"exam-grid-overcat\">%s</span>".format(getValueStringForRender)
	override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(ExamGridExportStyles.OvercatAndActualMark))
		} else {
			cell.setCellStyle(cellStyleMap(ExamGridExportStyles.Overcat))
		}
	}
}

case class ExamGridColumnValueOvercatDecimal(value: BigDecimal, override val isActual: Boolean = false)
	extends ExamGridColumnValueDecimal(value) with ExamGridColumnValueOvercat

case class ExamGridColumnValueOvercatString(value: String, override val isActual: Boolean = false)
	extends ExamGridColumnValueString(value) with ExamGridColumnValueOvercat


trait ExamGridColumnValueOverride {

	self: ExamGridColumnValue =>

	override def toHTML: String = "<span class=\"exam-grid-override\">%s</span>".format(getValueStringForRender)
	override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		cell.setCellStyle(cellStyleMap(ExamGridExportStyles.Overridden))
	}
}

case class ExamGridColumnValueOverrideDecimal(value: BigDecimal, override val isActual: Boolean = false)
	extends ExamGridColumnValueDecimal(value) with ExamGridColumnValueOverride

case class ExamGridColumnValueOverrideString(value: String, override val isActual: Boolean = false)
	extends ExamGridColumnValueString(value) with ExamGridColumnValueOverride


case class ExamGridColumnValueMissing(message: String = "") extends ExamGridColumnValueString("X", isActual = true) {
	override def toHTML: String = "<span class=\"exam-grid-actual-mark %s\" %s>X</span>".format(
		if (message.hasText) "use-tooltip" else "",
		if (message.hasText) "title=\"%s\" data-container=\"body\"".format(message) else ""
	)
	override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
		cell.setCellStyle(cellStyleMap(ExamGridExportStyles.ActualMark))
	}
}

case class ExamGridColumnValueStringWithHtml(value: String, html: String) extends ExamGridColumnValueString(value) {
	override def toHTML: String = html
}

case class ExamGridColumnValueStringHtmlOnly(value: String) extends ExamGridColumnValueString(value) {
	override def toHTML: String = super.toHTML
	override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {}
}
