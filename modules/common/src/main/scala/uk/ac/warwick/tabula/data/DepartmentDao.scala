package uk.ac.warwick.tabula.data
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import uk.ac.warwick.tabula.data.model.Department
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole

trait DepartmentDao {
	def allDepartments: Seq[Department]
	def getByCode(code: String): Option[Department]
	def getById(id: String): Option[Department]
	def save(department: Department)
}

@Repository
class DepartmentDaoImpl extends DepartmentDao with Daoisms {

	def allDepartments: Seq[Department] =
		session.newCriteria[Department]
			.addOrder(Order.asc("code"))
			.list

	// Fetches modules eagerly
	def getByCode(code: String) = 
		session.newQuery[Department]("from Department d left join fetch d.modules where d.code = :code")
			.setString("code", code)
			.uniqueResult
		
	def getById(id: String) = getById[Department](id)

	def save(department: Department) = session.saveOrUpdate(department)

}