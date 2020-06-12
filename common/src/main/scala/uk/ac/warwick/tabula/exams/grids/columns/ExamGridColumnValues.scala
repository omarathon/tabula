package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.commons.lang.StringEscapeUtils
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.{Cell, CellStyle}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridExportStyles
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
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

  override def toString: String = s"${getClass.getSimpleName}($getValueStringForRender)"

  protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit

  val isActual: Boolean

  val isFail: Boolean = false

  def toHTML: String

  def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle], commentHelper: SpreadsheetHelpers.CommentHelper): Unit

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
            case _: ExamGridColumnValueOvercat if values.tail.forall(_.isInstanceOf[ExamGridColumnValueOvercat]) =>
              ExamGridColumnValueOvercatString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
            case _: ExamGridColumnValueOverride if values.tail.forall(_.isInstanceOf[ExamGridColumnValueOverride]) =>
              ExamGridColumnValueOverrideString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
            case _: ExamGridColumnValueMissing if values.tail.forall(_.isInstanceOf[ExamGridColumnValueMissing]) =>
              ExamGridColumnValueMissing()
            case _ =>
              ExamGridColumnValueString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual, isFail = values.tail.forall(_.isFail))
          }
        } else {
          // If only some are actual we can't apply a common style, so just return a plain merged string
          ExamGridColumnValueString(values.map(_.getValueStringForRender).mkString(","))
        }
    }
  }
}

object ExamGridColumnGraduationBenchmarkDecimal {
  def apply(value: BigDecimal, isActual: Boolean = false, url: String) = new ExamGridColumnGraduationBenchmarkDecimal(value, isActual, url)
}

class ExamGridColumnGraduationBenchmarkDecimal(value: BigDecimal, override val isActual: Boolean = false, val url: String) extends ExamGridColumnValueDecimal(value, isActual) {
  override def toHTML: String = "<a href=\""+StringEscapeUtils.escapeHtml(url)+"\" target=\"_blank\">"+super.toHTML+"</a>"

  override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle], commentHelper: SpreadsheetHelpers.CommentHelper): Unit = {
    super.populateCell(cell, cellStyleMap, commentHelper)
    val link = cell.getSheet.getWorkbook.getCreationHelper.createHyperlink(HyperlinkType.URL)
    link.setAddress(url)
    cell.setHyperlink(link)
  }
}

object ExamGridColumnValueDecimal {
  def apply(value: BigDecimal, isActual: Boolean = false, isFail: Boolean = false) = new ExamGridColumnValueDecimal(value, isActual, isFail)
}

class ExamGridColumnValueDecimal(value: BigDecimal, val isActual: Boolean = false, override val isFail: Boolean = false) extends ExamGridColumnValue {
  protected final def getValueForRender: JBigDecimal = value.underlying.stripTrailingZeros()

  override protected final def getValueStringForRender: String = getValueForRender.toPlainString

  override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
    (isActual, isFail) match {
      case (true, true) => cell.setCellStyle(cellStyleMap(ExamGridExportStyles.FailAndActualMark))
      case (true, false) => cell.setCellStyle(cellStyleMap(ExamGridExportStyles.ActualMark))
      case (false, true) => cell.setCellStyle(cellStyleMap(ExamGridExportStyles.Fail))
      case (false, false) => // no specific style
    }
  }

  override def toHTML: String = {
    val actualClass = if (isActual) "exam-grid-actual-mark" else ""
    val failedClass = if (isFail) "exam-grid-fail" else ""
    s"""<span class="$actualClass $failedClass">$getValueStringForRender</span>"""
  }

  override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle], commentHelper: SpreadsheetHelpers.CommentHelper): Unit = {
    cell.setCellValue(getValueForRender.doubleValue)
    applyCellStyle(cell, cellStyleMap)
  }

  override def isEmpty: Boolean = value == null
}

object ExamGridColumnValueString {
  def apply(value: String, isActual: Boolean = false, isFail: Boolean = false) = new ExamGridColumnValueString(value, isActual, isFail)
}

class ExamGridColumnValueString(value: String, val isActual: Boolean = false, override val isFail: Boolean = false) extends ExamGridColumnValue {
  override protected final def getValueStringForRender: String = value

  override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {

    (isActual, isFail) match {
      case (true, true) => cell.setCellStyle(cellStyleMap(ExamGridExportStyles.FailAndActualMark))
      case (true, false) => cell.setCellStyle(cellStyleMap(ExamGridExportStyles.ActualMark))
      case (false, true) => cell.setCellStyle(cellStyleMap(ExamGridExportStyles.Fail))
      case (false, false) => cell.setCellStyle(cellStyleMap(ExamGridExportStyles.WrappedText))
    }
  }

  override def toHTML: String = {
    val actualClass = if (isActual) "exam-grid-actual-mark" else ""
    val failedClass = if (isFail) "exam-grid-fail" else ""
    s"""<span class="$actualClass $failedClass">$value</span>"""
  }

  override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle], commentHelper: SpreadsheetHelpers.CommentHelper): Unit = {
    cell.setCellValue(value)
    applyCellStyle(cell, cellStyleMap)
  }

  override def isEmpty: Boolean = !value.hasText
}

case class ExamGridColumnValueWithTooltip(value: String, actual: Boolean, message: String = "", failed: Boolean = false) extends ExamGridColumnValueString(value, actual, failed) {
  override def toHTML: String = {
    val actualClass = if (isActual) "exam-grid-actual-mark" else ""
    val failedClass = if (isFail) "exam-grid-fail" else ""
    val (tooltipClass, tooltipMessage) = if (message.hasText) ("tabula-tooltip", s"""tabindex="0" data-title="$message" """) else ("", "")
    s"""<span class="$actualClass $failedClass $tooltipClass" $tooltipMessage>$value</span>"""
  }

  override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle], commentHelper: SpreadsheetHelpers.CommentHelper): Unit = {
    super.populateCell(cell, cellStyleMap, commentHelper)

    if (message.hasText)
      cell.setCellComment(commentHelper.createComment(cell, message))
  }
}


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
  override def toHTML: String =
    if (message.hasText)
      s"""<span class="exam-grid-actual-mark tabula-tooltip" tabindex="0" data-title="$message">X</span>"""
    else
      """<span class="exam-grid-actual-mark">X</span>"""

  override protected def applyCellStyle(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle]): Unit = {
    cell.setCellStyle(cellStyleMap(ExamGridExportStyles.ActualMark))
  }

  override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle], commentHelper: SpreadsheetHelpers.CommentHelper): Unit = {
    super.populateCell(cell, cellStyleMap, commentHelper)

    if (message.hasText)
      cell.setCellComment(commentHelper.createComment(cell, message))
  }
}

case class ExamGridColumnValueStringWithHtml(value: String, html: String) extends ExamGridColumnValueString(value) {
  override def toHTML: String = html
}

case class ExamGridColumnValueStringHtmlOnly(value: String) extends ExamGridColumnValueString(value) {
  override def toHTML: String = super.toHTML

  override def populateCell(cell: Cell, cellStyleMap: Map[ExamGridExportStyles.Style, CellStyle], commentHelper: SpreadsheetHelpers.CommentHelper): Unit = {}
}
