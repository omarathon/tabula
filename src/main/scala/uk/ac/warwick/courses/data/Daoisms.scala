package uk.ac.warwick.courses.data
import org.hibernate.SessionFactory
import scala.reflect.Manifest
import org.springframework.beans.factory.annotation.Autowired
import scala.annotation.target.field

trait Daoisms {
  type AutowiredField = Autowired @field
  
	
  @AutowiredField var sessionFactory:SessionFactory = _
  protected def session = sessionFactory.getCurrentSession
  
  /**
   * Returns Some(obj) if it matches the expected type, otherwise None.
   * Useful for converting the value from .uniqueResult into a typed Option.
   * 
   * An implicit Manifest object is supplied by the Scala compiler, which
   * holds detailed information about the type D which is otherwise missing
   * from the JVM bytecode.
   */
  def option[D] (obj:Any): Option[D] = obj match {
	  case a:D => Some[D](a)
	  case _ => None
  }

}