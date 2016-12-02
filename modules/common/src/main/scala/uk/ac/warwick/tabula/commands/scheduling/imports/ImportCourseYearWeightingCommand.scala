package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportAcademicInformationCommand.ImportResult
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.CourseYearWeighting
import uk.ac.warwick.tabula.data.{CourseDao, Daoisms}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions

class ImportCourseYearWeightingCommand(courseCode: String, academicYear: AcademicYear, yearOfStudy: Int, weighting: BigDecimal)
	extends Command[(CourseYearWeighting, ImportAcademicInformationCommand.ImportResult)] with Logging with Daoisms
		with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var courseDao: CourseDao = Wire.auto[CourseDao]

	override def applyInternal(): (CourseYearWeighting, ImportResult) = transactional() {
		val existingWeighting = courseDao.getCourseYearWeighting(courseCode, academicYear, yearOfStudy)
		val course = courseDao.getByCode(courseCode)

		if (course.isEmpty) {

			logger.error(s"Could not import course year weighting as course $courseCode does not exist")
			(null, ImportAcademicInformationCommand.ImportResult())

		} else {

			logger.debug(s"Importing course year weighting $courseCode, ${academicYear.toString}, ${yearOfStudy.toString}, ${weighting.toString}")

			val (newWeighting, hasChanged, isTransient) = existingWeighting match {
				case Some(courseYearWeighting: CourseYearWeighting) =>
					if (courseYearWeighting.weighting != weighting.underlying) {
						courseYearWeighting.weighting = weighting.underlying
						(courseYearWeighting, true, false)
					} else {
						(courseYearWeighting, false, false)
					}
				case _ =>
					val courseYearWeighting = new CourseYearWeighting()
					courseYearWeighting.course = course.get
					courseYearWeighting.academicYear = academicYear
					courseYearWeighting.yearOfStudy = yearOfStudy
					courseYearWeighting.weighting = weighting.underlying
					(courseYearWeighting, true, true)
			}

			if (hasChanged) {
				logger.debug("Saving changes for " + newWeighting)
				courseDao.saveOrUpdate(newWeighting)
			}

			val result =
				if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
				else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
				else ImportAcademicInformationCommand.ImportResult()

			(newWeighting, result)

		}
	}

	override def describe(d: Description): Unit = d.properties(
		"courseCode" -> courseCode,
		"academicYear" -> academicYear.toString,
		"yearOfStudy" -> yearOfStudy.toString,
		"weighting" -> weighting.toString
	)

}
