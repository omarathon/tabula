package uk.ac.warwick.tabula.dev.web.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.TermServiceImpl
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentMember
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.ModuleRegistration

class ModuleRegistrationFixtureCommand extends CommandInternal[Unit] with Logging {
	this: SessionComponent with TransactionalComponent  =>

	var memberDao: MemberDao = Wire[MemberDaoImpl]
	var moduleDao: ModuleDao = Wire[ModuleDaoImpl]

	var moduleCode: String = _
	var universityIds: String = _

	protected def applyInternal() {
		transactional() {

			val universityIdSeq = universityIds.split(",")

			val module = moduleDao.getByCode(moduleCode)
			val cats = new java.math.BigDecimal(12.0)

			for (uniId <- universityIdSeq) {
				memberDao.getByUniversityId(uniId) match {
					case Some(stu: StudentMember) => {
						for (scd <- stu.studentCourseDetails.asScala) {
							val modReg = new ModuleRegistration(scd, module.get, cats, AcademicYear(2013))
							session.save(modReg)
							scd.moduleRegistrations.add(modReg)
							session.save(scd)
						}
					}
					case _ => None
				}

			}
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
