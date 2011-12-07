package uk.ac.warwick.courses.data
import org.hibernate.SessionFactory
import scala.reflect.Manifest
import org.springframework.beans.factory.annotation.Autowired
import scala.annotation.target.field

/**
 * A trait for DAO classes to mix in to get useful things
 * like the current session.
 */
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
  def option[D] (obj:Any)(implicit m:Manifest[D]) : Option[D] = obj match {
	  case a:Any if m.erasure.isInstance(a) => Some(a.asInstanceOf[D])
	  case _ => None
  }
  
  // type-safe session.get. returns an Option object, which will match None if
  // null was returned.
  protected def getById[D](id:String)(implicit m:Manifest[D]) : Option[D] =
	  Option(session.get(m.erasure.getName(), id).asInstanceOf[D])

}