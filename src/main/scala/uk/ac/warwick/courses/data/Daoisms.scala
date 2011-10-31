package uk.ac.warwick.courses.data
import org.hibernate.SessionFactory

import scala.reflect.Manifest

trait Daoisms {
  
  val sessionFactory:SessionFactory
  protected def session = sessionFactory.getCurrentSession
  
  /**
   * Returns Some(obj) if it matches the expected type, otherwise None.
   * Useful for converting the value from .uniqueResult into an Option.
   * 
   * Messed around a bit with some Scala type features here. As the JVM
   * doesn't know what generics are, regular checks against type D aren't
   * actually checked. Scala adds a thing called a Manifest which can store
   * all this stuff, so if one is passed in to the method call, it can ask
   * the Manifest all sorts of things about the type.
   * 
   * It would be painful to have to pass in the Manifest when calling the
   * method so we can use another Scala feature, implicit parameters. When
   * a Manifest is defined as implicit it will be automatically added by
   * the compiler.
   */
  def option[D] (obj:Any) (implicit m:Manifest[D]) :Option[D] = 
    if (m.erasure.isInstance(obj)) {
    	Some(obj.asInstanceOf[D])
    } else {
    	None
    }
}