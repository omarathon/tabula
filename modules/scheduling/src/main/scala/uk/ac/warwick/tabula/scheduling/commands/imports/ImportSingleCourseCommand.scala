package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.CourseDao
import uk.ac.warwick.tabula.data.model.Course

class ImportSingleCourseCommand(resultSet: ResultSet) extends Command[Course] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var courseDao = Wire.auto[CourseDao]

	var code = resultSet.getString("crs_code")
	var shortName = resultSet.getString("crs_snam")
	var name = resultSet.getString("crs_name")
	var title = resultSet.getString("crs_titl")

	override def applyInternal(): Course = transactional() {
		val courseExisting = courseDao.getByCode(code)

		logger.debug("Importing course " + code + " into " + courseExisting)

		val isTransient = !courseExisting.isDefined

		val course = courseExisting match {
			case Some(crs: Course) => crs
			case _ => new Course()
		}

		val commandBean = new BeanWrapperImpl(this)
		val courseBean = new BeanWrapperImpl(course)

		val hasChanged = copyBasicProperties(properties, commandBean, courseBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + course)

			course.lastUpdatedDate = DateTime.now
			courseDao.saveOrUpdate(course)
		}

		course
	}

	private val properties = Set(
		"code", "shortName", "name", "title"
	)

	override def describe(d: Description) = d.property("shortName" -> shortName)

}
