package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionAndFeedbackCommand.Student
import uk.ac.warwick.tabula.data.model.{Exam, ExamFeedback, Module}
import uk.ac.warwick.tabula.exams.commands.ViewExamCommandResult
import uk.ac.warwick.util.csv.CSVLineWriter

trait ExamExports {

	class CSVBuilder(val items:Seq[Student], val results:ViewExamCommandResult, val exam: Exam, val module: Module, val academicYear: AcademicYear)
		extends CSVLineWriter[Student] with ExamCsvExport {

		var topLevelUrl: String = Wire.property("${toplevel.url}")

		def getNoOfColumns(item:Student) = headers.size
		def getColumn(item:Student, i:Int) = formatData(itemData(item).get(headers(i)))

		def itemData(student: Student) = {
			val warwickId = student.user.getWarwickId
			val seatNumber = results.seatNumberMap.get(student.user)
			var hasFeedback:Boolean = false
			var feedback = new ExamFeedback

			if (!results.feedbackMap.get(student.user).isEmpty) {
				hasFeedback = true;
				feedback  = results.feedbackMap.get(student.user).get.get
			}

			val hasSitsStatus =  hasFeedback && results.sitsStatusMap.get(feedback).size > 0

			var studentName = ""
			if (module.adminDepartment.showStudentName) {
				studentName = student.user.getFullName
			} else {
				studentName = warwickId
			}

			var data:Map[String, Any] = Map();
			data += (SEAT_NUMBER -> seatNumber)
			data += (STUDENT -> studentName)
			if (hasFeedback) {
				data += (ORIGINAL_MARK -> feedback.actualMark.get)
				data += (ORIGINAL_GRADE -> feedback.actualGrade.get)
				data += (ADJUSTED_MARK -> feedback.latestPrivateOrNonPrivateAdjustment.get.mark)
				data += (ADJUSTED_GRADE -> feedback.latestPrivateOrNonPrivateAdjustment.get.grade)
				if (hasSitsStatus) {
					val sitsStatus = results.sitsStatusMap.get(feedback).get.get
					data += (SITS_UPLOAD_STATUS -> sitsStatus.status.description)
					data += (SITS_UPLOAD_DATE -> sitsStatus.dateOfUpload)
					data += (SITS_UPLOAD_MARK -> sitsStatus.actualMarkLastUploaded)
					data += (SITS_UPLOAD_GRADE -> sitsStatus.actualGradeLastUploaded)
				}
			}
			data
		}

		protected def formatData(data: Option[Any]) = data match {
			case Some(date: DateTime) => DateTimeFormat.forPattern("HH:mm:ss dd/MM/yyyy").print(date)
			case Some(b: Boolean) => b.toString.toLowerCase
			case Some(i: Int) => i.toString
			case Some(s: String) => s
			case Some(Some(s: String)) => s
			case Some(Some(Some(i:Int))) => i.toString
			case None => ""
			case _ => ""
		}
	}
}

trait ExamCsvExport {

	val SEAT_NUMBER: String = "Seat number"
	val STUDENT: String = "Student"
	val ORIGINAL_MARK: String = "Original Mark"
	val ORIGINAL_GRADE: String = "Original Grade"
	val ADJUSTED_MARK: String = "Adjusted Mark"
	val ADJUSTED_GRADE: String = "Adjusted Grade"
	val SITS_UPLOAD_STATUS: String = "SITS upload Status"
	val SITS_UPLOAD_DATE: String = "SITS upload Date"
	val SITS_UPLOAD_MARK: String = "SITS upload Mark"
  val SITS_UPLOAD_GRADE: String = "SITS upload Grade"

	var headers:Seq[String] = List(
		SEAT_NUMBER,
		STUDENT,
		ORIGINAL_MARK,
		ORIGINAL_GRADE,
		ADJUSTED_MARK,
		ADJUSTED_GRADE,
		SITS_UPLOAD_STATUS,
		SITS_UPLOAD_DATE,
		SITS_UPLOAD_MARK,
		SITS_UPLOAD_GRADE
	)
}

