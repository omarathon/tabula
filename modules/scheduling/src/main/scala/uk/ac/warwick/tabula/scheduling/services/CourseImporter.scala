package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import scala.collection.JavaConversions._
import javax.sql.DataSource
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.CourseDao
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportCourseCommand
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.scheduling.sandbox.SandboxData

trait CourseImporter extends Logging {
	var courseDao = Wire[CourseDao]
	
	private var courseMap: Map[String, Course] = _

	def getCourseForCode(code: String) = {
		if (courseMap == null) updateCourseMap()
		courseMap(code)
	}

	protected def updateCourseMap() {
		courseMap = slurpCourses()
	}

	private def slurpCourses(): Map[String, Course] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS course map")

			(for {
				courseCode <- courseDao.getAllCourseCodes
				course <- courseDao.getByCode(courseCode)
			} yield (courseCode -> course)).toMap

		}
	}

	def importCourses() {
		logger.info("Importing Courses")

		getImportCommands foreach { _.apply() }

		updateCourseMap()
	}
	
	def getImportCommands: Seq[ImportCourseCommand]
}

@Profile(Array("dev", "test", "production")) @Service
class SitsCourseImporter extends CourseImporter {
	import SitsCourseImporter._

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val coursesQuery = new CoursesQuery(sits)

	def getImportCommands: Seq[ImportCourseCommand] = {
		coursesQuery.execute.toSeq
	}
}

object SitsCourseImporter {

	val GetCourse = """
		select crs_code, crs_snam, crs_name, crs_titl from intuit.srs_crs
		"""

	class CoursesQuery(ds: DataSource) extends MappingSqlQuery[ImportCourseCommand](ds, GetCourse) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = 
			new ImportCourseCommand(
				CourseInfo(
					code=resultSet.getString("crs_code"),
					shortName=resultSet.getString("crs_snam"),
					fullName=resultSet.getString("crs_name"),
					title=resultSet.getString("crs_titl")
				)
			)
	}

}

case class CourseInfo(code: String, shortName: String, fullName: String, title: String)

@Profile(Array("sandbox")) @Service
class SandboxCourseImporter extends CourseImporter {

	def getImportCommands: Seq[ImportCourseCommand] =
		SandboxData.Departments
			.flatMap { case (departmentCode, department) =>  
				department.routes.map { case (routeCode, route) =>
					new ImportCourseCommand(
						CourseInfo(
							code="%s-%s".format(departmentCode.toUpperCase, routeCode.toUpperCase),
							shortName=route.name.substring(0, 20).toUpperCase,
							fullName=route.name,
							title="%s %s".format(route.degreeType.description, route.name)
						)
					)
				}
			}
			.toSeq
	
}
