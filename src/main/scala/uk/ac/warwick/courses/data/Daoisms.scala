package uk.ac.warwick.courses.data
import org.hibernate.SessionFactory

trait Daoisms {
  val sessionFactory:SessionFactory
  protected def session = sessionFactory.getCurrentSession
  
  /**
   * Returns Some(obj) if it matches the expected type, otherwise None.
   * Useful for converting the value from .uniqueResult into an Option.
   */
  def option[D<:Any](obj:Any) = obj match {
    case obj:D => Some(obj)
    case _ => None
  }
}