package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.{Course, DegreeType, Route}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class CourseCreationFixtureCommand extends CommandInternal[Unit] {
	this: ModuleAndDepartmentServiceComponent with SessionComponent with TransactionalComponent =>

	var courseDao:CourseDao = Wire[CourseDao]
	var courseCode: String = _
	var courseName: String = _

	protected def applyInternal() {
		transactional() {
			val c = courseDao.getByCode(courseCode).getOrElse(new Course)
			c.code = courseCode
			c.name = courseName
			c.shortName = courseName
			c.title = courseName

			courseDao.saveOrUpdate(c)
		}
	}
}

object CourseCreationFixtureCommand{
	def apply()={
		new CourseCreationFixtureCommand
			with ComposableCommand[Unit]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions

	}
}

