package uk.ac.warwick.tabula.commands.reports.profiles

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.{AttendanceNote, FileAttachment, StudentMember}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringUserLookupComponent, TermServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object ProfileExportSingleCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new ProfileExportSingleCommandInternal(student, academicYear)
			with AutowiredTextRendererComponent
			with FreemarkerXHTMLPDFGeneratorComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringUserLookupComponent
			with ComposableCommand[Seq[FileAttachment]]
			with ProfileExportSingleDescription
			with ProfileExportSinglePermissions
			with ProfileExportSingleCommandState
}


class ProfileExportSingleCommandInternal(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[Seq[FileAttachment]] with TaskBenchmarking {

	self: FreemarkerXHTMLPDFGeneratorComponent with AttendanceMonitoringServiceComponent
		with TermServiceComponent with UserLookupComponent =>

	var fileDao = Wire.auto[FileDao]

	case class PointData(
		departmentName: String,
		term: String,
		state: String,
		name: String,
		pointType: String,
		pointTypeInfo: String,
		startDate: LocalDate,
		endDate: LocalDate,
		recordedBy: User,
		recordedDate: DateTime,
		attendanceNote: Option[AttendanceNote]
	)

	override def applyInternal() = {
		// Get point data
		val pointData = if (academicYear.startYear < 2014) {
			getOldPointData
		} else {
			getPointData
		}

		// Get coursework
		// Get small groups
		// Get meetings
		// Build model
		val groupedPoints = pointData
			.groupBy(_.departmentName).mapValues(_
			.groupBy(_.state).mapValues(_
			.groupBy(_.term)))

		// Render PDF
		val tempOutputStream = new ByteArrayOutputStream()
		pdfGenerator.renderTemplate(
			"/WEB-INF/freemarker/reports/profile-export.ftl",
			Map(
				"groupedPoints" -> groupedPoints
			),
			tempOutputStream
		)

		// Create file
		val pdfFileAttachment = new FileAttachment
		pdfFileAttachment.name = s"${student.universityId}-profile.pdf"
		pdfFileAttachment.uploadedData = new ByteArrayInputStream(tempOutputStream.toByteArray)
		pdfFileAttachment.uploadedDataLength = 0
		fileDao.saveTemporary(pdfFileAttachment)

		// Return results
		Seq(pdfFileAttachment)
	}

	def getOldPointData: Seq[PointData] = {
		Seq()
	}

	def getPointData: Seq[PointData] = {
		val checkpoints = benchmarkTask("attendanceMonitoringService.getAllAttendance") {
			attendanceMonitoringService.getAllAttendance(student.universityId)
		}
		val attendanceNoteMap = benchmarkTask("attendanceMonitoringService.getAttendanceNoteMap") {
			attendanceMonitoringService.getAttendanceNoteMap(student)
		}
		val users = benchmarkTask("userLookup.getUsersByUserIds") {
			userLookup.getUsersByUserIds(checkpoints.map(_.updatedBy).asJava).asScala
		}
		checkpoints.map(checkpoint => {
			PointData(
				checkpoint.point.scheme.department.name,
				termService.getTermFromDateIncludingVacations(checkpoint.point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString,
				checkpoint.state.dbValue,
				checkpoint.point.name,
				checkpoint.point.pointType.description,
				"",
				checkpoint.point.startDate,
				checkpoint.point.endDate,
				users(checkpoint.updatedBy),
				checkpoint.updatedDate,
				attendanceNoteMap.get(checkpoint.point)
			)
		})
	}

}

trait ProfileExportSinglePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ProfileExportSingleCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Reports, student)
	}

}

trait ProfileExportSingleDescription extends Describable[Seq[FileAttachment]] {

	self: ProfileExportSingleCommandState =>

	override lazy val eventName = "ProfileExportSingle"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
	}
}

trait ProfileExportSingleCommandState {
	def student: StudentMember
	def academicYear: AcademicYear
}
