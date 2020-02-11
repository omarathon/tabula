package uk.ac.warwick.tabula.commands.attendance.view

import org.apache.poi.ss.usermodel.{Row, Sheet}
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.{XSSFDataValidation, XSSFDataValidationConstraint, XSSFDataValidationHelper}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.JavaImports.JInteger
import uk.ac.warwick.tabula.commands.profiles.relationships.StudentRelationshipTemplateCommand
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.jdk.CollectionConverters._

object RecordMonitoringPointTemplateCommand {

  val StudentColumnHeader = "student_id"
  val StudentNameColumnHeader = "student_name"
  val AttendanceColumnHeader = "attendance"

  def apply(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint, user: CurrentUser) =
    new RecordMonitoringPointTemplateCommandInternal(department, academicYear, templatePoint, user)
      with ComposableCommand[ExcelView]
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringProfileServiceComponent
      with RecordMonitoringPointTemplatePermissions
      with RecordMonitoringPointCommandState
      with PopulateRecordMonitoringPointCommand
      with SetFilterPointsResultOnRecordMonitoringPointCommand
      with ReadOnly with Unaudited

}

class RecordMonitoringPointTemplateCommandInternal(val department: Department, val academicYear: AcademicYear, val templatePoint: AttendanceMonitoringPoint, val user: CurrentUser)
  extends CommandInternal[ExcelView]  {

  self: RecordMonitoringPointCommandState with AttendanceMonitoringServiceComponent =>

  def applyInternal(): ExcelView = {
    val workbook = generateWorkbook()
    new ExcelView(s"Record attendance for ${templatePoint.name}.xlsx", workbook)
  }

  def generateWorkbook(): SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = generateAttendanceSheet(workbook)

    val dropdownRange = new CellRangeAddressList(1, checkpointMap.size, 2, 2)
    val dvHelper = new XSSFDataValidationHelper(null)
    val dvConstraint = dvHelper.createExplicitListConstraint(
      Array(AttendanceState.NotRecorded.dbValue,
        AttendanceState.Attended.dbValue,
        AttendanceState.MissedAuthorised.dbValue,
        AttendanceState.MissedUnauthorised.dbValue)
    ).asInstanceOf[XSSFDataValidationConstraint]

    val validation = dvHelper.createValidation(dvConstraint, dropdownRange).asInstanceOf[XSSFDataValidation]
    validation.setShowErrorBox(true)
    sheet.addValidationData(validation)

    for ((studentMember, checkpoint) <- checkpointMap.asScala) {
      val row = sheet.createRow(sheet.getLastRowNum + 1)
      row.createCell(0).setCellValue(studentMember.universityId)
      row.createCell(1).setCellValue(studentMember.fullName.getOrElse(""))

      checkpoint.get(templatePoint) match {
        case state: AttendanceState => createUnprotectedCell(workbook, row, 2, state.dbValue)
        case _ => createUnprotectedCell(workbook, row, 2, AttendanceState.NotRecorded.dbValue)
      }
    }
    workbook
  }

  private def generateAttendanceSheet(workbook: SXSSFWorkbook): Sheet = {
    val sheet = workbook.createSheet("attendance")
    sheet.trackColumnsForAutoSizing((0 to 3).map(i => i: JInteger).asJava)

    // add header row
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue(RecordMonitoringPointTemplateCommand.StudentColumnHeader)
    header.createCell(1).setCellValue(RecordMonitoringPointTemplateCommand.StudentNameColumnHeader)
    header.createCell(2).setCellValue(RecordMonitoringPointTemplateCommand.AttendanceColumnHeader)

    // using apache-poi, we can't protect certain cells - rather we have to protect
    // the entire sheet and then unprotect the ones we want to remain editable
    sheet.protectSheet(StudentRelationshipTemplateCommand.sheetPassword)
    sheet
  }

  private def createUnprotectedCell(workbook: SXSSFWorkbook, row: Row, col: Int, value: String = "") = {
    val lockedCellStyle = workbook.createCellStyle()
    lockedCellStyle.setLocked(false)
    val cell = row.createCell(col)
    cell.setCellValue(value)
    cell.setCellStyle(lockedCellStyle)
    cell
  }

  }

trait RecordMonitoringPointTemplateCommandState {
  def department: Department
  def academicYear: AcademicYear
  def templatePoint: AttendanceMonitoringPoint
}

trait RecordMonitoringPointTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RecordMonitoringPointCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MonitoringPoints.Record, department)
  }
}
