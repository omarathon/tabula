package uk.ac.warwick.tabula.commands.marks

import org.apache.poi.ss.usermodel.{ComparisonOperator, IndexedColors, Row, Sheet}
import org.apache.poi.ss.util.{CellRangeAddress, WorkbookUtil}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ModuleMarksTemplateCommand._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.marks.{AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.web.views.ExcelView

object ModuleMarksTemplateCommand {
  type Result = ExcelView
  type Command = Appliable[Result]

  val SheetPassword = "roygbiv"

  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String): Command =
    new ModuleMarksTemplateCommandInternal(sitsModuleCode, module, academicYear, occurrence)
      with CalculateModuleMarksLoadModuleRegistrations
      with ModuleOccurrenceUpdateMarksPermissions
      with CalculateModuleMarksAlgorithm
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with ComposableCommand[Result] // late-init due to CalculateModuleMarksLoadModuleRegistrations being called from permissions
      with Unaudited with ReadOnly
}

abstract class ModuleMarksTemplateCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String)
  extends CommandInternal[Result]
    with ModuleOccurrenceState {
  self: CalculateModuleMarksLoadModuleRegistrations =>

  override def applyInternal(): Result = {
    val workbook = new SXSSFWorkbook
    val fullSheetName = s"Marks for $sitsModuleCode ${module.name} (${academicYear.toString}, $occurrence)"
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
    header.createCell(0).setCellValue("SPR Code")
    header.createCell(1).setCellValue("Mark")
    header.createCell(2).setCellValue("Grade")
    header.createCell(3).setCellValue("Result")
    header.createCell(4).setCellValue("Comments")

    // populate the mark sheet with ids and existing data
    studentModuleMarkRecordsAndCalculations.zipWithIndex.foreach { case ((student, _, _, _), i) =>
      val row = sheet.createRow(i + 1)
      row.createCell(0).setCellValue(student.sprCode)

      val markCell = createUnprotectedCell(row, 1)
      val gradeCell = createUnprotectedCell(row, 2)
      val resultCell = createUnprotectedCell(row, 3)
      createUnprotectedCell(row, 4)

      student.mark.foreach(markCell.setCellValue(_))
      student.grade.foreach(gradeCell.setCellValue)
      student.result.foreach(r => resultCell.setCellValue(r.dbValue))
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
