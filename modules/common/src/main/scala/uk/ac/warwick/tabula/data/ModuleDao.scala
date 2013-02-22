package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import scala.collection.JavaConversions._
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._
import org.springframework.beans.factory.annotation.Autowired
import collection.JavaConverters._
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
	def findByParticipant(userId: String): Seq[Module]
	def findByParticipant(userId: String, dept: Department): Seq[Module]
}

@Repository
class ModuleDaoImpl extends ModuleDao with Daoisms {

	def allModules: Seq[Module] =
		session.newCriteria[Module]
			.addOrder(Order.asc("code"))
			.list

	def saveOrUpdate(module: Module) = session.saveOrUpdate(module)

	def getByCode(code: String) = option[Module] {
		session.createQuery("from Module m where code = :code").setString("code", code).uniqueResult
	}
	
	def getById(id: String) = getById[Module](id)

	/**
	 * TODO This doesn't understand WebGroup-based permissions or custom roles that are based off ModuleManager.
	 */
	def findByParticipant(userId: String): Seq[Module] =
		session.createQuery("""
				from Module m 
				left join fetch m.grantedRoles r
				where r.builtInRoleDefinition = :def 
					and :user in elements(r.users.includeUsers)
				""")
			.setParameter("def", ModuleManagerRoleDefinition)
			.setString("user", userId)
			.list.asInstanceOf[JList[Module]]

	/**
	 * Find modules managed by this user, in this department.
	 * 
	 * TODO This doesn't understand WebGroup-based permissions or custom roles that are based off ModuleManager.
	 */
	def findByParticipant(userId: String, dept: Department): Seq[Module] =
		findByParticipant(userId) filter { _.department == dept }

}