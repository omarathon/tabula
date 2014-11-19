package uk.ac.warwick.tabula.reports.commands.attendancemonitoring

import freemarker.template.DefaultObjectWrapper
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState.NotRecorded
import uk.ac.warwick.tabula.helpers.IntervalFormatter
import uk.ac.warwick.util.csv.CSVLineWriter

class AllAttendanceReportExporter(val processorResult: AllAttendanceReportProcessorResult) extends CSVLineWriter[AttendanceMonitoringStudentData] {

	val intervalFormatter = new IntervalFormatter
	val wrapper = new DefaultObjectWrapper()

	val result = processorResult.result
	val students = processorResult.students
	val points = processorResult.points

	val headers = Seq("First name","Last name","University ID") ++
		points.map(p => s"${p.name} (${intervalFormatter.exec(JList(
			wrapper.wrap(p.startDate.toDate),
			wrapper.wrap(p.endDate.toDate)
		)).asInstanceOf[String].replaceAll("<sup>","").replaceAll("</sup>","")})") ++
		Seq("Unrecorded","Missed (unauthorised)")

	val unrecordedIndex = headers.size - 2
	val missedIndex = headers.size - 1

	override def getNoOfColumns(o: AttendanceMonitoringStudentData): Int = headers.size

	override def getColumn(studentData: AttendanceMonitoringStudentData, pointIndex: Int): String = {
		pointIndex match {
			case 0 =>
				studentData.firstName
			case 1 =>
				studentData.lastName
			case 2 =>
				studentData.universityId
			case index if index == unrecordedIndex =>
				result(studentData).map { case (point, state) => state}.count(_ == NotRecorded).toString
			case index if index == missedIndex =>
				result(studentData).map { case (point, state) => state}.count(_ == NotRecorded).toString
			case _ =>
				val thisPoint = points(pointIndex - 3)
				result(studentData).get(thisPoint).map{
					case state if state == NotRecorded =>
						if (thisPoint.isLate)
							"Late"
						else
							state.description
					case state =>
						state.description
				}.getOrElse("n/a")
		}
	}
}
