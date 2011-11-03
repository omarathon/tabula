package uk.ac.warwick.courses.data
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import uk.ac.warwick.courses.data.model.Department
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order
import scala.collection.JavaConversions._
import java.util.{List => JList}
import collection.JavaConverters._

trait DepartmentDao {
  def allDepartments: Seq[Department]
  def getByCode(code:String): Option[Department]
  def save(department:Department)
  def getByOwner(user:String):Seq[Department]
}
@Repository
class DepartmentDaoImpl extends DepartmentDao with Daoisms {
  
  def allDepartments: Seq[Department] = 
    session.createCriteria(classOf[Department])
    	.addOrder(Order.asc("code"))
    	.list.asInstanceOf[JList[Department]]
  
  // Fetches modules eagerly
  def getByCode(code:String) = option[Department](
      session.createQuery("from Department d left join fetch d.modules where d.code = :code").setString("code",code).uniqueResult
  )
    
  def save(department:Department) = session.saveOrUpdate(department)
  
  /**
   * Get all departments owned by a particular usercode.
   * Doesn't work with UserGroups that use a webgroup (it's not possible in the UI
   * to use a webgroup for the department admins list.)
   */
  def getByOwner(user:String): Seq[Department] = {
      val query = session.createQuery("from Department d where :user in elements(d.owners.includeUsers)")
      query.setString("user", user)
      query.list.asInstanceOf[JList[Department]]
  }
}