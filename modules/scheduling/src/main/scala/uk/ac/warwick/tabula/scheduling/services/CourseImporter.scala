package uk.ac.warwick.tabula.scheduling.services

import java.sql.ResultSet
import scala.collection.JavaConversions._
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleSitsStatusCommand
import org.apache.log4j.Logger
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.CourseDao
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportSingleCourseCommand
import uk.ac.warwick.tabula.data.Daoisms

@Service
class CourseImporter extends Logging with Daoisms {
	import CourseImporter._

	var courseDao = Wire.auto[CourseDao]

	var sits = Wire[DataSource]("sitsDataSource")

	lazy val coursesQuery = new CoursesQuery(sits)

	val courseMap = importCourses

	def importCourses = {
		logger.info("Importing Courses")

		transactional() {
			getCourses map { _.apply }
			session.flush
			session.clear
		}

		slurpCourses
	}

	def getCourses: Seq[ImportSingleCourseCommand] = {
		val courses = coursesQuery.execute.toSeq
		courses
	}

	def slurpCourses: Map[String, Course] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS course map")

			(for (courseCode <- courseDao.getAllCourseCodes; course <- courseDao.getByCode(courseCode)) yield {
				(courseCode, course)
			}).toMap
		}
	}

}

object CourseImporter {

	val GetCourse = """
		select crs_code, crs_snam, crs_name, crs_titl from intuit.srs_crs
		"""

	class CoursesQuery(ds: DataSource) extends MappingSqlQuery[ImportSingleCourseCommand](ds, GetCourse) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) = new ImportSingleCourseCommand(resultSet)
	}

}
