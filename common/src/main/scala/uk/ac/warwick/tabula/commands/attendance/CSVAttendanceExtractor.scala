package uk.ac.warwick.tabula.commands.attendance

import java.nio.charset.StandardCharsets

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.helpers.DetectMimeType
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.util.csv.{GoodCsvDocument, NamedValueCSVLineReader}

import scala.collection.JavaConverters._
import scala.util.Try

object CSVAttendanceExtractor {
	def apply() =
		new CSVAttendanceExtractorInternal
		with AutowiringProfileServiceComponent
}

class CSVAttendanceExtractorInternal extends BindListener {

	self: ProfileServiceComponent =>

	var file: UploadedFile = new UploadedFile

	override def onBind(result: BindingResult): Unit = {
		file.onBind(result)
	}

	private def detectMimeType(uploadedFile: UploadedFile) = DetectMimeType.detectMimeType(uploadedFile.attached.get(0).asByteSource.openStream())

	def extract(errors: Errors): Map[StudentMember, AttendanceState] = {
		if (file.attached.isEmpty) {
			errors.reject("file.missing")
			Map()
		} else if (!detectMimeType(file).startsWith("text/")) {
			errors.reject("file.format.csv")
			Map()
		} else {
			val reader = new NamedValueCSVLineReader
			reader.setHasHeaders(false)
			val doc = new GoodCsvDocument[JList[String]](null, reader)
			doc.read(file.attached.get(0).asByteSource.asCharSource(StandardCharsets.UTF_8).openStream())

			val rows: Seq[Map[String, String]] = reader.getData.asScala.map(_.asScala.toMap)
			val goodRows = rows.filter { row =>
				if (row.keys.size != 2) {
					errors.reject("attendanceMonitoringCheckpoint.upload.wrongFields", Array(row.values.mkString(",")), "")
					false
				} else if (Try(AttendanceState.fromCode(row("1"))).isFailure) {
					errors.reject("attendanceMonitoringCheckpoint.upload.wrongState", Array(row.values.mkString(",")), "")
					false
				} else {
					true
				}
			}
			if (goodRows.isEmpty) {
				Map()
			} else {
				val uniIDs = goodRows.map(_ ("0"))
				val students = profileService.getAllMembersWithUniversityIds(uniIDs).collect { case (s: StudentMember) => s }
				val studentRows = goodRows.filter { row =>
					students.find(_.universityId == row("0")) match {
						case None =>
							errors.reject("attendanceMonitoringCheckpoint.upload.notStudent", Array(row.values.mkString(",")), "")
							false
						case _ =>
							true
					}
				}
				studentRows.map(row => students.find(_.universityId == row("0")).get -> AttendanceState.fromCode(row("1"))).toMap
			}
		}
	}

}
