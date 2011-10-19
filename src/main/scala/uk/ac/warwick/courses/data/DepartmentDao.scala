package uk.ac.warwick.courses.data
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import uk.ac.warwick.courses.data.model.Department
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Order
import scala.collection.JavaConversions._
import java.util.{List => JList}

trait DepartmentDao {
  def allDepartments: Seq[Department]
  def getDepartmentByCode(code:String): Option[Department]
  def save(department:Department)
}
@Repository
class DepartmentDaoImpl @Autowired()(val sessionFactory:SessionFactory) extends DepartmentDao with Daoisms {
  
  def allDepartments: Seq[Department] = 
    session.createCriteria(classOf[Department])
    	.addOrder(Order.asc("code"))
    	.list.asInstanceOf[JList[Department]]
  
  // Fetches modules eagerly
  def getDepartmentByCode(code:String) = option[Department](
      session.createQuery("from Department d left join fetch d.modules where d.code = :code").setString("code",code).uniqueResult
  )
    
  def save(department:Department) = session.saveOrUpdate(department)
}