package uk.ac.warwick.tabula.commands.marks

import org.apache.poi.ss.usermodel.{ComparisonOperator, IndexedColors, Row, Sheet}
import org.apache.poi.ss.util.{CellRangeAddress, WorkbookUtil}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.AssessmentComponentMarksTemplateCommand._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup, UpstreamAssessmentGroupInfo}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.web.views.ExcelView

object AssessmentComponentMarksTemplateCommand {
  type Result = ExcelView
  type Command = Appliable[Result]

  val SheetPassword = "roygbiv"

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): Command =
    new AssessmentComponentMarksTemplateCommandInternal(assessmentComponent, upstreamAssessmentGroup)
      with ComposableCommand[Result]
      with RecordAssessmentComponentMarksPermissions
      with Unaudited with ReadOnly
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
}

abstract class AssessmentComponentMarksTemplateCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState {
  self: AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): Result = {
    val info = UpstreamAssessmentGroupInfo(
      upstreamAssessmentGroup,
      assessmentMembershipService.getCurrentUpstreamAssessmentGroupMembers(upstreamAssessmentGroup.id)
    )

    val students = ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService)

    val workbook = new SXSSFWorkbook
    val fullSheetName = s"Marks for ${assessmentComponent.name} (${assessmentComponent.moduleCode}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})"
    val sheetName = WorkbookUtil.createSafeSheetName(fullSheetName)
    val sheet = workbook.createSheet(sheetName)

    val lockedCellStyle = workbook.createCellStyle()
    lockedCellStyle.setLocked(false)

    def createUnprotectedCell(row: Row, col: Int) = {
      val cell = row.createCell(col)
      cell.setCellStyle(lockedCellStyle)
      cell
    }

    // using apache-poi, we can't protect certain cells - rather we have to protect
    // the entire sheet and then unprotect the ones we want to remain editable
    sheet.protectSheet(SheetPassword)

    // add header row
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("University ID")
    header.createCell(1).setCellValue("Mark")
    header.createCell(2).setCellValue("Grade")
    header.createCell(3).setCellValue("Comments")

    // populate the mark sheet with ids and existing data
    students.zipWithIndex.foreach { case (student, i) =>
      val row = sheet.createRow(i + 1)
      row.createCell(0).setCellValue(student.universityId)

      val markCell = createUnprotectedCell(row, 1)
      val gradeCell = createUnprotectedCell(row, 2)
      createUnprotectedCell(row, 3)

      student.mark.foreach(markCell.setCellValue(_))
      student.grade.foreach(gradeCell.setCellValue)
    }

    def addConditionalFormatting(sheet: Sheet): Unit = {
      val sheetCF = sheet.getSheetConditionalFormatting

      val invalidMarkRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_BETWEEN, "0", "100")
      val fontFmt = invalidMarkRule.createFontFormatting
      fontFmt.setFontStyle(true, false)
      fontFmt.setFontColorIndex(IndexedColors.DARK_RED.index)

      val marksColumn = Array(new CellRangeAddress(1, sheet.getLastRowNum, 1, 1))
      sheetCF.addConditionalFormatting(marksColumn, invalidMarkRule)
    }

    // add conditional formatting for invalid marks
    if (sheet.getLastRowNum > 0) addConditionalFormatting(sheet)
    new ExcelView(s"$fullSheetName.xlsx", workbook)
  }
}
