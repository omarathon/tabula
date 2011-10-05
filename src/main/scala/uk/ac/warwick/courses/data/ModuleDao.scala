package uk.ac.warwick.courses.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._
import org.springframework.beans.factory.annotation.Autowired

trait ModuleDao {
  def saveOrUpdate(module:Module)
  def getByCode(code:String):Module
}

@Repository
class ModuleDaoImpl @Autowired()(sessionFactory:SessionFactory) extends Object with ModuleDao {
  
  def saveOrUpdate(module:Module) = session.saveOrUpdate(module)
  def getByCode(code:String) = session.find("select from Module as module where code = ?", code, new StringType) match {
    case list:List[Module] if !list.isEmpty => list.get(0).asInstanceOf[Module]
  }

  private def session = sessionFactory.getCurrentSession
  
}