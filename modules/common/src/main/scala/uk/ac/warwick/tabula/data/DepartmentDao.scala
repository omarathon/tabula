package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.Department
import org.hibernate.criterion.Order
import scala.collection.JavaConversions._

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
			.distinct

	// Fetches modules eagerly
	def getByCode(code: String) =
		session.newQuery[Department]("from Department d left join fetch d.modules where d.code = :code")
			.setString("code", code.toLowerCase())
			.uniqueResult

	def getById(id: String) = getById[Department](id)

	def save(department: Department) = session.saveOrUpdate(department)

}