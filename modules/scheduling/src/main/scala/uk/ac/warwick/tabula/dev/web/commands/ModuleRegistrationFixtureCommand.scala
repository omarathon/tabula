package uk.ac.warwick.tabula.dev.web.commands

import scala.collection.JavaConverters.asScalaBufferConverter

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, Daoisms, MemberDao, MemberDaoImpl, ModuleDao, ModuleDaoImpl, SessionComponent, TransactionalComponent}
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

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
