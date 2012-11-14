package uk.ac.warwick.tabula.data
import org.hibernate.event._

/**
 * Method for Hibernate to call whenever it loads an object from the database.
 */

trait PreLoadBehaviour {
	def preLoad
}

/** Happens before save, BUT changes to properties here won't get saved! Rendering it quite useless. */
trait PreSaveBehaviour {
	def preSave(newRecord: Boolean)
}

trait PostLoadBehaviour {
	def postLoad
}

class HibernateLifecycle extends PostLoadEventListener with PreLoadEventListener with PreInsertEventListener with PreUpdateEventListener {
	override def onPostLoad(event: PostLoadEvent) {
		event.getEntity match {
			case listener: PostLoadBehaviour => listener.postLoad
			case _ =>
		}
	}

	override def onPreInsert(event: PreInsertEvent) = {
		event.getEntity match {
			case listener: PreSaveBehaviour => listener.preSave(true)
			case _ =>
		}
		false
	}

	override def onPreUpdate(event: PreUpdateEvent) = {
		event.getEntity match {
			case listener: PreSaveBehaviour => listener.preSave(false)
			case _ =>
		}
		false
	}

	override def onPreLoad(event: PreLoadEvent) {
		event.getEntity match {
			case listener: PreLoadBehaviour => listener.preLoad
			case _ =>
		}
	}
}

