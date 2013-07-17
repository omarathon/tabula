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

@Service
class CourseImporter extends Logging with Daoisms {
	import CourseImporter._

	var courseDao = Wire.auto[CourseDao]

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val coursesQuery = new CoursesQuery(sits)

	private var courseMap: Map[String, Course] = _

	private def updateCourseMap() {
		courseMap = slurpCourses()
	}

	def getCourseForCode(code: String) = {
		if (courseMap == null) updateCourseMap()
		courseMap(code)
	}

	def importCourses() {
		logger.info("Importing Courses")

		transactional() {
			getImportCommands foreach { _.apply() }
			session.flush()
			session.clear()
		}

		updateCourseMap()
	}

	def getImportCommands: Seq[ImportCourseCommand] = {
		coursesQuery.execute.toSeq
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

}

object CourseImporter {

	val GetCourse = """
		select crs_code, crs_snam, crs_name, crs_titl from intuit.srs_crs
		"""

	class CoursesQuery(ds: DataSource) extends MappingSqlQuery[ImportCourseCommand](ds, GetCourse) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = new ImportCourseCommand(resultSet)
	}

}
