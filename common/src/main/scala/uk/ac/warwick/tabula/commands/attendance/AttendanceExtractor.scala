package uk.ac.warwick.tabula.commands.attendance

import java.nio.charset.StandardCharsets

import org.apache.commons.io.FilenameUtils._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.commands.attendance.view.RecordMonitoringPointTemplateCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.helpers.{DetectMimeType, SpreadsheetHelpers}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.util.csv.{GoodCsvDocument, NamedValueCSVLineReader}

import scala.jdk.CollectionConverters._
import scala.util.Try

object AttendanceExtractor {
  def apply() =
    new AttendanceExtractorInternal
      with AutowiringProfileServiceComponent
}

class AttendanceExtractorInternal extends BindListener {

  self: ProfileServiceComponent =>

  var file: UploadedFile = new UploadedFile

  override def onBind(result: BindingResult): Unit = {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
  }

  private def detectMimeType(uploadedFile: UploadedFile) =
    DetectMimeType.detectMimeType(uploadedFile.attached.get(0).asByteSource.openStream())

  def extract(errors: Errors): Map[StudentMember, AttendanceState] = {
    if (file.attached.isEmpty) {
      errors.reject("file.missing")
      Map()
    } else if (file.fileNames.forall(name => getExtension(name) == "xlsx")) {
      val parsedRows = SpreadsheetHelpers.parseXSSFExcelFile(file.attached.get(0).asByteSource.openStream())
      val rowsData = parsedRows.map(_.data.-(RecordMonitoringPointTemplateCommand.StudentNameColumnHeader))
      validateAndExtractAttendance(rowsData, errors, RecordMonitoringPointTemplateCommand.StudentColumnHeader, RecordMonitoringPointTemplateCommand.AttendanceColumnHeader)
    } else if (detectMimeType(file).getType != "text") {
      errors.reject("file.format.csv")
      Map()
    } else {
      val reader = new NamedValueCSVLineReader
      reader.setHasHeaders(false)
      val doc = new GoodCsvDocument[JList[String]](null, reader)
      doc.read(file.attached.get(0).asByteSource.asCharSource(StandardCharsets.UTF_8).openStream())

      val rows: Seq[Map[String, String]] = reader.getData.asScala.toSeq.map(_.asScala.toMap)
      validateAndExtractAttendance(rows, errors, "0", "1")
    }
  }

  private def validateAndExtractAttendance(rows: Seq[Map[String, String]], errors: Errors, studentColumnIdentifier: String, attendanceColumnIdentifier: String):  Map[StudentMember, AttendanceState] = {
    val goodRows = rows.filter { row =>
      if (row.keys.size != 2) {
        errors.reject("attendanceMonitoringCheckpoint.upload.wrongFields", Array(row.values.mkString(",")), "")
        false
      } else if (Try(AttendanceState.fromCode(row(attendanceColumnIdentifier))).isFailure) {
        errors.reject("attendanceMonitoringCheckpoint.upload.wrongState", Array(row.values.mkString(",")), "")
        false
      } else {
        true
      }
    }
    if (goodRows.isEmpty) {
      Map()
    } else {
      val uniIDs = goodRows.map(_ (studentColumnIdentifier))
      val students = profileService.getAllMembersWithUniversityIds(uniIDs).collect { case s: StudentMember => s }
      val studentRows = goodRows.filter { row =>
        students.find(_.universityId == row(studentColumnIdentifier)) match {
          case None =>
            errors.reject("attendanceMonitoringCheckpoint.upload.notStudent", Array(row.values.mkString(",")), "")
            false
          case _ =>
            true
        }
      }
      studentRows.map(row => students.find(_.universityId == row(studentColumnIdentifier)).get -> AttendanceState.fromCode(row(attendanceColumnIdentifier))).toMap
    }
  }
}
