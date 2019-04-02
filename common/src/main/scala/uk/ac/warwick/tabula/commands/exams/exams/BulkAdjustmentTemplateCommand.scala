package uk.ac.warwick.tabula.commands.exams.exams

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{Assessment, Assignment, Exam, Feedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

object BulkAdjustmentTemplateCommand {
  def apply(assessment: Assessment) =
    new BulkAdjustmentTemplateCommandInternal(assessment)
      with AutowiringAssessmentMembershipServiceComponent
      with ComposableCommand[ExcelView]
      with BulkAdjustmentTemplatePermissions
      with BulkAdjustmentTemplateCommandState
      with ReadOnly with Unaudited
}


class BulkAdjustmentTemplateCommandInternal(val assessment: Assessment) extends CommandInternal[ExcelView] {

  self: AssessmentMembershipServiceComponent =>
  override def applyInternal(): ExcelView = {
    val workbook = generateWorkbook
    new ExcelView("Adjustments for " + assessment.name + ".xlsx", workbook)
  }

  private def generateWorkbook: SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = workbook.createSheet("Marks")
    sheet.trackAllColumnsForAutoSizing()

    val markerFields: Seq[String] = assessment match {

      case a: Assignment if a.cm2Assignment && a.cm2MarkingWorkflow != null => a.cm2MarkingWorkflow.allocationOrder
      case a: Assignment if a.markingWorkflow != null => Seq("first-marker", "second-marker")
      case _ => Seq()
    }

    // add header row
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue(BulkAdjustmentCommand.StudentIdHeader)
    markerFields.zipWithIndex.foreach{case (mf, i) => header.createCell(i+1).setCellValue(mf) }
    header.createCell(markerFields.size + 1).setCellValue("Original mark")
    header.createCell(markerFields.size + 2).setCellValue("Original grade")
    header.createCell(markerFields.size + 3).setCellValue("Previous adjusted mark (if any)")
    header.createCell(markerFields.size + 4).setCellValue("Previous adjusted grade (if any)")
    header.createCell(markerFields.size + 5).setCellValue(BulkAdjustmentCommand.MarkHeader)
    header.createCell(markerFields.size + 6).setCellValue(BulkAdjustmentCommand.GradeHeader)
    header.createCell(markerFields.size + 7).setCellValue(BulkAdjustmentCommand.ReasonHeader)
    header.createCell(markerFields.size + 8).setCellValue(BulkAdjustmentCommand.CommentsHeader)

    val memberOrder = assessmentMembershipService.determineMembershipUsers(assessment)
      .zipWithIndex.toMap.map { case (user, order) => user.getWarwickId -> order }

    def markerData(feedback: Feedback): Seq[String] = assessment match {
      case a: Assignment if a.cm2Assignment && a.cm2MarkingWorkflow != null => a.cm2MarkingWorkflow.allocationOrder.map(role => {
        feedback.feedbackMarkerByAllocationName(role).map(_.getFullName).getOrElse("")
      })
      case a: Assignment if a.markingWorkflow != null => Seq(
        a.getStudentsFirstMarker(feedback.studentIdentifier).map(_.getFullName).getOrElse(""),
        a.getStudentsSecondMarker(feedback.studentIdentifier).map(_.getFullName).getOrElse("")
      )
      case _ => Seq()
    }

    assessment.fullFeedback
      .sortBy(f => {
        val order = for (uniId <- f.universityId; o <- memberOrder.get(uniId)) yield o
        order.getOrElse(10000)
      })
      .foreach(f => {
        val row = sheet.createRow(sheet.getLastRowNum + 1)
        val md = markerData(f)
        row.createCell(0).setCellValue(f.studentIdentifier)
        md.zipWithIndex.foreach {case (mf, i) => row.createCell(i+1).setCellValue(mf)}
        row.createCell(md.size + 1).setCellValue(f.actualMark.map(_.toString).getOrElse(""))
        row.createCell(md.size + 2).setCellValue(f.actualGrade.getOrElse(""))
        row.createCell(md.size + 3).setCellValue(f.latestPrivateOrNonPrivateAdjustment.map(_.mark.toString).getOrElse(""))
        row.createCell(md.size + 4).setCellValue(
          f.latestPrivateOrNonPrivateAdjustment.flatMap(_.grade.map(_.toString)).getOrElse("")
        )
      })

    val style = workbook.createCellStyle
    // using an @ sets text format (from BuiltinFormats.class)
    style.setDataFormat(workbook.createDataFormat.getFormat("@"))
    sheet.setDefaultColumnStyle(0, style)

    0 to 8 foreach sheet.autoSizeColumn

    workbook
  }

}

trait BulkAdjustmentTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: BulkAdjustmentTemplateCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    HibernateHelpers.initialiseAndUnproxy(mandatory(assessment)) match {
      case assignment: Assignment =>
        p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
      case exam: Exam =>
        p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
    }
  }

}

trait BulkAdjustmentTemplateCommandState {
  def assessment: Assessment
}
