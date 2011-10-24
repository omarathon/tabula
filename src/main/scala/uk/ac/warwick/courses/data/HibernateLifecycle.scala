package uk.ac.warwick.courses.data
import org.hibernate.event._

/**
 * Method for Hibernate to call whenever it loads an object from the database.
 */

trait PreLoadBehaviour { 
  def preLoad 
}

class HibernateLifecycle extends PostLoadEventListener with PreLoadEventListener {
	override def onPostLoad(event: PostLoadEvent) {
	  event.getEntity match {
	    case listener:PostLoadBehaviour => listener.postLoad
	    case _ =>
	  }
	}
	
	override def onPreLoad(event: PreLoadEvent) {
	  event.getEntity match {
	    case listener:PreLoadBehaviour => listener.preLoad
	    case _ =>
	  }
	}
}


