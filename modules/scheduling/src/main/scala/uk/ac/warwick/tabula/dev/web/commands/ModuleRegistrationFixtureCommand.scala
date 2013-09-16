package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.{UpstreamAssessmentGroup, UserGroup}
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{TermServiceImpl}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, Daoisms, TransactionalComponent, SessionComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.JavaImports.JArrayList

class ModuleRegistrationFixtureCommand extends CommandInternal[Unit] with Logging {
	this: SessionComponent with TransactionalComponent=>

	var moduleCode: String = _
	var universityIds: String = _

	protected def applyInternal() {
		transactional() {
			val group = UserGroup.ofUniversityIds
			group.staticIncludeUsers = JArrayList(universityIds.split(",") :_*)
			val uag = new UpstreamAssessmentGroup
			uag.members = group
			uag.moduleCode = moduleCode
			uag.assessmentGroup = "A"
			uag.occurrence = "A"
			uag.academicYear = AcademicYear.findAcademicYearContainingDate(DateTime.now, new TermServiceImpl())
			session.save(uag)
		}

	}
}
object ModuleRegistrationFixtureCommand{
	def apply()={
		new ModuleRegistrationFixtureCommand
			with ComposableCommand[Unit]
			with Daoisms
		  with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions

	}
}
