package uk.ac.warwick.tabula.commands.coursework.feedback

import org.apache.poi.ss.usermodel.{ComparisonOperator, IndexedColors, Sheet}
import org.apache.poi.ss.util.{CellRangeAddress, WorkbookUtil}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assessment, Assignment, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, FeedbackServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object OldGenerateOwnMarksTemplateCommand {
	def apply(module: Module, assignment: Assignment, members: Seq[String]) =
		new OldGenerateMarksTemplateCommandInternal(module, assignment, members)
			with AutowiringFeedbackServiceComponent
			with ComposableCommand[SXSSFWorkbook]
			with OldGenerateOwnMarksTemplatePermissions
			with OldGenerateMarksTemplateCommandState
			with Unaudited with ReadOnly
}

object OldGenerateMarksTemplateCommand {
	def apply(module: Module, assignment: Assignment, members: Seq[String]) =
		new OldGenerateMarksTemplateCommandInternal(module, assignment, members)
			with AutowiringFeedbackServiceComponent
			with ComposableCommand[SXSSFWorkbook]
			with OldGenerateAllMarksTemplatePermissions
			with OldGenerateMarksTemplateCommandState
			with Unaudited with ReadOnly
}

object OldMarksTemplateCommand {

	// util to replace unsafe characters with spaces
	def safeAssessmentName(assessment: Assessment): String = WorkbookUtil.createSafeSheetName(trimmedAssessmentName(assessment))

	val MaxSpreadsheetNameLength = 31
	val MaxAssignmentNameLength: Int = MaxSpreadsheetNameLength - "Marks for ".length

	// trim the assignment name down to 21 characters. Excel sheet names must be 31 chars or less so
	// "Marks for " = 10 chars + assignment name (max 21) = 31
	def trimmedAssessmentName(assessment: Assessment): String = {
		if (assessment.name.length > MaxAssignmentNameLength)
			assessment.name.substring(0, MaxAssignmentNameLength)
		else
			assessment.name
	}

}

class OldGenerateMarksTemplateCommandInternal(val module: Module, val assignment: Assignment, val members: Seq[String]) extends CommandInternal[SXSSFWorkbook] {

	self: FeedbackServiceComponent =>

	override def applyInternal(): SXSSFWorkbook = {

		val workbook = new SXSSFWorkbook
		val sheet = generateNewMarkSheet(assignment, workbook)

		// populate the mark sheet with ids
		for ((member, i) <- members.zipWithIndex) {
			val row = sheet.createRow(i + 1)
			row.createCell(0).setCellValue(member)
		}

		// add conditional formatting for invalid marks
		if (sheet.getLastRowNum > 0) addConditionalFormatting(sheet)

		workbook
	}

	private def generateNewMarkSheet(assignment: Assignment, workbook: SXSSFWorkbook) = {
		val sheet = workbook.createSheet("Marks for " + OldMarksTemplateCommand.safeAssessmentName(assignment))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("University ID")
		header.createCell(1).setCellValue("Mark")
		// TODO could perhaps have it calculated based on the grade boundaries
		header.createCell(2).setCellValue("Grade")

		sheet
	}

	private def addConditionalFormatting(sheet: Sheet) = {
		val sheetCF = sheet.getSheetConditionalFormatting

		val invalidMarkRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_BETWEEN, "0", "100")
		val fontFmt = invalidMarkRule.createFontFormatting
		fontFmt.setFontStyle(true, false)
		fontFmt.setFontColorIndex(IndexedColors.DARK_RED.index)

		val marksColumn = Array(new CellRangeAddress(1, sheet.getLastRowNum, 1, 1))
		sheetCF.addConditionalFormatting(marksColumn, invalidMarkRule)
	}

}

trait OldGenerateMarksTemplateCommandState {
	def module: Module
	def assignment: Assignment
}

trait OldGenerateOwnMarksTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: OldGenerateMarksTemplateCommandState =>

	mustBeLinked(assignment, module)

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.DownloadMarksTemplate, assignment)
	}

}

trait OldGenerateAllMarksTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: OldGenerateMarksTemplateCommandState =>

	mustBeLinked(assignment, module)

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.DownloadMarksTemplate, assignment)
	}

}
