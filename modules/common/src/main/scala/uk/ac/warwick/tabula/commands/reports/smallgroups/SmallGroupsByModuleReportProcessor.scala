package uk.ac.warwick.tabula.commands.reports.smallgroups

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.commands.reports.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.services.{ProfileServiceComponent, AutowiringProfileServiceComponent, AutowiringTermServiceComponent, TermServiceComponent}

import scala.collection.JavaConverters._

object SmallGroupsByModuleReportProcessor {
	def apply(department: Department, academicYear: AcademicYear) =
		new SmallGroupsByModuleReportProcessorInternal(department, academicYear)
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[SmallGroupsByModuleReportProcessorResult]
			with ReportPermissions
			with SmallGroupsByModuleReportProcessorState
			with ReadOnly with Unaudited {
			override lazy val eventName: String = "SmallGroupsByModuleReportProcessor"
		}
}

case class ModuleData(
	id: String,
	code: String,
	name: String
)

case class SmallGroupsByModuleReportProcessorResult(
	counts: Map[AttendanceMonitoringStudentData, Map[ModuleData, Int]],
	students: Seq[AttendanceMonitoringStudentData],
	modules: Seq[ModuleData]
)

class SmallGroupsByModuleReportProcessorInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[SmallGroupsByModuleReportProcessorResult] with TaskBenchmarking {

	self: SmallGroupsByModuleReportProcessorState with TermServiceComponent with ProfileServiceComponent =>

	override def applyInternal() = {
		val processedStudents = students.asScala.map{properties =>
			val scd = profileService.getMemberByUniversityId(properties.get("universityId")) match {
				case Some (student: StudentMember) => Some(student.mostSignificantCourse)
				case _ => None
			}
			AttendanceMonitoringStudentData(
				properties.get("firstName"),
				properties.get("lastName"),
				properties.get("universityId"),
				null,
				null,
				null,
				scd.map(_.currentRoute.code).getOrElse(""),
				null,
				scd.map(_.latestStudentCourseYearDetails.yearOfStudy.toString).getOrElse(""),
				scd.map(_.sprCode).getOrElse("")
			)
		}.toSeq.sortBy(s => (s.lastName, s.firstName))
		val processedModules = modules.asScala.map{properties =>
			ModuleData(
				properties.get("id"),
				properties.get("code"),
				properties.get("name")
			)
		}.toSeq.sortBy(_.code)
		val processedCounts = counts.asScala.flatMap{case(universityId, moduleMap) =>
			processedStudents.find(_.universityId == universityId).map(studentData =>
				studentData -> moduleMap.asScala.flatMap { case (id, countString) =>
					processedModules.find(_.id == id).map(module => module -> countString.toInt)
				}.toMap)
		}.toMap
		SmallGroupsByModuleReportProcessorResult(processedCounts, processedStudents, processedModules)
	}

}

trait SmallGroupsByModuleReportProcessorState extends ReportCommandState {
	var counts: JMap[String, JMap[String, String]] =
		LazyMaps.create{_: String => JMap[String, String]() }.asJava

	var students: JList[JMap[String, String]] = JArrayList()

	var modules: JList[JMap[String, String]] = JArrayList()
}
