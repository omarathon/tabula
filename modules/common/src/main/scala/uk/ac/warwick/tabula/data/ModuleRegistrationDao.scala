package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.ModuleRegistration

trait ModuleRegistrationDao {
	def saveOrUpdate(moduleRegistration: ModuleRegistration)
	def getBySprCode(sprCode: String): Seq[String]
	//def getByNotionalKey(sprCode: String, moduleCode: String, cats: Double, academicYear: AcademicYear): Option[ModuleRegistration]
	def getByNotionalKey(sprCode: String, moduleCode: String, cats: Double, academicYear: AcademicYear): Option[ModuleRegistration]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

	def saveOrUpdate(moduleRegistration: ModuleRegistration) = session.saveOrUpdate(moduleRegistration)

/*	def getByNotionalKey(sprCode: String, moduleCode: String, academicYear: AcademicYear, cats: java.math.BigDecimal) = {
		session.newCriteria[ModuleRegistration]
			//.add(is("sprCode", sprCode))
			//.add(is("moduleCode", moduleCode))
			//.add(is("academicYear", academicYear))
			//.add(is("cats", cats))
			.uniqueResult
	}*/

	def getBySprCode(sprCode: String) =
		session.newQuery[String] ("""
				select
					sprCode
				from
					ModuleRegistration mr
				where
					mr.sprCode = :sprCode
				""")
			.setParameter("sprCode", sprCode)
			.seq

	def getByNotionalKey(sprCode: String, moduleCode: String, cats: Double, academicYear: AcademicYear) =

		session.newQuery[ModuleRegistration] ("""
				select
					mr
				from
					ModuleRegistration mr
				where
					mr.sprCode = :sprCode
				and
					mr.moduleCode = :moduleCode
				and
					mr.academicYear = :academicYear
				and
					mr.cats = :cats
				""")
			.setParameter("sprCode", sprCode)
			.setParameter("moduleCode", moduleCode)
			.setParameter("academicYear", academicYear)
			.setParameter("cats", cats)
			.uniqueResult
}
