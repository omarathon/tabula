package uk.ac.warwick.tabula.commands.cm2.feedback

import org.apache.poi.ss.usermodel.{ComparisonOperator, IndexedColors, Row, Sheet}
import org.apache.poi.ss.util.{CellRangeAddress, WorkbookUtil}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.AssignmentAnonymity.FullyAnonymous
import uk.ac.warwick.tabula.data.model.{Assessment, Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.helpers.UserOrderingByIds._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object GenerateOwnMarksTemplateCommand {
	def apply(assignment: Assignment, marker: User) =
		new GenerateOwnMarksTemplateCommandInternal(assignment, marker)
			with AutowiringFeedbackServiceComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
			with ComposableCommand[SXSSFWorkbook]
			with GenerateOwnMarksTemplatePermissions
			with GenerateMarksTemplateCommandState
			with Unaudited with ReadOnly
}

object GenerateMarksTemplateCommand {
	def apply(assignment: Assignment) =
		new GenerateMarksTemplateCommandInternal(assignment)
			with AutowiringFeedbackServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with ComposableCommand[SXSSFWorkbook]
			with GenerateMarksTemplatePermissions
			with GenerateMarksTemplateCommandState
			with Unaudited with ReadOnly
}

object MarksTemplateCommand {

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

class GenerateOwnMarksTemplateCommandInternal(val assignment: Assignment, val marker: User) extends CommandInternal[SXSSFWorkbook] {

	self: FeedbackServiceComponent with CM2MarkingWorkflowServiceComponent with GenerateMarksTemplateCommandState =>

	private val sheetPassword = "roygbiv"

	override def applyInternal(): SXSSFWorkbook = {

		val students: Seq[User] = cm2MarkingWorkflowService.getAllStudentsForMarker(assignment, marker).sorted
		val markerFeedbackToDo = students.flatMap(s => {
			val feedback = assignment.allFeedback.find(_.usercode == s.getUserId)
			feedback.flatMap(f => f.markerFeedback.asScala.find(mf => marker == mf.marker && f.outstandingStages.asScala.contains(mf.stage)))
		})

		val workbook = new SXSSFWorkbook
		val sheet =  workbook.createSheet("Marks for " + MarksTemplateCommand.safeAssessmentName(assignment))

		val lockedCellStyle = workbook.createCellStyle()
		lockedCellStyle.setLocked(false)

		def createUnprotectedCell(row: Row, col: Int) = {
			val cell = row.createCell(col)
			cell.setCellStyle(lockedCellStyle)
			cell
		}

		// using apache-poi, we can't protect certain cells - rather we have to protect
		// the entire sheet and then unprotect the ones we want to remain editable
		sheet.protectSheet(sheetPassword)

		// add header row
		val header = sheet.createRow(0)
		val idHeader = if(assignment.anonymity == FullyAnonymous) "ID" else "University ID"
		header.createCell(0).setCellValue(idHeader)
		header.createCell(1).setCellValue("Mark")
		header.createCell(2).setCellValue("Grade")
		header.createCell(3).setCellValue("Feedback")

		val maxCurrentStage = markerFeedbackToDo.map(_.feedback.currentStageIndex).max
		val stages = assignment.cm2MarkingWorkflow.allStages.filter(_.order < maxCurrentStage)
		for((stage, i) <- stages.zipWithIndex){
			val cell = 3 + ((i+1)*2)
			header.createCell(cell-1).setCellValue(stage.description)
			header.createCell(cell).setCellValue(s"${stage.description} mark")
		}

		// populate the mark sheet with ids and existing data
		for ((currentMarkerFeedback, i) <- markerFeedbackToDo.zipWithIndex) {
			val feedback = currentMarkerFeedback.feedback
			val previousMarkerFeedback: Seq[MarkerFeedback] = feedback.markerFeedback.asScala.filter(_.stage.order < feedback.currentStageIndex)

			val row = sheet.createRow(i + 1)
			val id = if(assignment.anonymity == FullyAnonymous) {
				feedback.anonymousId.map(_.toString).getOrElse("")
			} else {
				Option(currentMarkerFeedback.student.getWarwickId).getOrElse(currentMarkerFeedback.student.getUserId)
			}
			row.createCell(0).setCellValue(id)

			val markCell = createUnprotectedCell(row, 1)
			val gradeCell = createUnprotectedCell(row, 2)
			val commentsCell = createUnprotectedCell(row, 3)

			for (mark <- currentMarkerFeedback.mark) markCell.setCellValue(mark)
			for (grade <- currentMarkerFeedback.grade) gradeCell.setCellValue(grade)
			for (comments <- currentMarkerFeedback.comments) commentsCell.setCellValue(comments)

			for((stage, i) <- stages.zipWithIndex){
				val cell = 3 + ((i+1)*2)
				val pmf = previousMarkerFeedback.find(_.stage == stage)
				row.createCell(cell-1).setCellValue(pmf.map(_.marker).map(_.getFullName).getOrElse(""))
				row.createCell(cell).setCellValue(pmf.flatMap(_.mark).map(_.toString).getOrElse(""))
			}
		}

		// add conditional formatting for invalid marks
		if (sheet.getLastRowNum > 0) addConditionalFormatting(sheet)
		workbook
	}
}

class GenerateMarksTemplateCommandInternal(val assignment: Assignment) extends CommandInternal[SXSSFWorkbook] {

	self: FeedbackServiceComponent with AssessmentMembershipServiceComponent with GenerateMarksTemplateCommandState =>

	override def applyInternal(): SXSSFWorkbook = {

		val students = assessmentMembershipService.determineMembershipUsers(assignment).sorted
		val workbook = new SXSSFWorkbook
		val sheet =  workbook.createSheet("Marks for " + MarksTemplateCommand.safeAssessmentName(assignment))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("University ID")
		header.createCell(1).setCellValue("Mark")
		header.createCell(2).setCellValue("Grade")
		header.createCell(3).setCellValue("Feedback")

		// populate the mark sheet with ids and existing data
		for ((member, i) <- students.zipWithIndex) {
			val feedback = assignment.allFeedback.find(_.usercode == member.getUserId)
			val row = sheet.createRow(i + 1)
			val id = Option(member.getWarwickId).getOrElse(member.getUserId)
			row.createCell(0).setCellValue(id)
			for (f <- feedback; mark <- f.actualMark) row.createCell(1).setCellValue(mark)
			for (f <- feedback; grade <- f.actualGrade) row.createCell(2).setCellValue(grade)
			for (f <- feedback; comments <- f.comments) row.createCell(3).setCellValue(comments)
		}

		// add conditional formatting for invalid marks
		if (sheet.getLastRowNum > 0) addConditionalFormatting(sheet)

		workbook
	}
}

trait GenerateMarksTemplateCommandState {
	def assignment: Assignment
	def addConditionalFormatting(sheet: Sheet): Unit = {
		val sheetCF = sheet.getSheetConditionalFormatting

		val invalidMarkRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_BETWEEN, "0", "100")
		val fontFmt = invalidMarkRule.createFontFormatting
		fontFmt.setFontStyle(true, false)
		fontFmt.setFontColorIndex(IndexedColors.DARK_RED.index)

		val marksColumn = Array(new CellRangeAddress(1, sheet.getLastRowNum, 1, 1))
		sheetCF.addConditionalFormatting(marksColumn, invalidMarkRule)
	}
}

trait GenerateOwnMarksTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateMarksTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.DownloadMarksTemplate, assignment)
	}

}

trait GenerateMarksTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateMarksTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.DownloadMarksTemplate, assignment)
	}

}
