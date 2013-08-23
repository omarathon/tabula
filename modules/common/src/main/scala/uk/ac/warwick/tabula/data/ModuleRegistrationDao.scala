package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._
import org.springframework.beans.factory.annotation.Autowired
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.AcademicYear

trait ModuleRegistrationDao {
	def saveOrUpdate(moduleRegistration: ModuleRegistration)
	def getByNotionalKey(sprCode: String, moduleCode: String, academicYear: AcademicYear, cats: String): Option[ModuleRegistration]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

	def saveOrUpdate(moduleRegistration: ModuleRegistration) = session.saveOrUpdate(moduleRegistration)

	def getByNotionalKey(sprCode: String, moduleCode: String, academicYear: AcademicYear, cats: String) =
		session.newCriteria[ModuleRegistration]
			.add(is("sprCode", sprCode))
			.add(is("moduleCode", moduleCode))
			.add(is("academicYear", academicYear))
			.add(is("cats", cats))
			.uniqueResult
}
