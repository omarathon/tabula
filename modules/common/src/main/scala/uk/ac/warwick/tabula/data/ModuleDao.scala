package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.JavaImports._
import model.Department
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole

trait ModuleDao {
	def allModules: Seq[Module]
	def saveOrUpdate(module: Module)
	def getByCode(code: String): Option[Module]
	def getById(id: String): Option[Module]
}

@Repository
class ModuleDaoImpl extends ModuleDao with Daoisms {

	def allModules: Seq[Module] =
		session.newCriteria[Module]
			.addOrder(Order.asc("code"))
			.seq
			.distinct

	def saveOrUpdate(module: Module) = session.saveOrUpdate(module)

	def getByCode(code: String) = 
		session.newQuery[Module]("from Module m where code = :code").setString("code", code).uniqueResult
	
	def getById(id: String) = getById[Module](id)

}