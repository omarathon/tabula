package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import javax.persistence._
import scala.reflect.BeanProperty

trait SettingsMap[T <: SettingsMap[T]] { self: T =>
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	@BeanProperty var settings: Map[String, Any] = Map()
	
	def -=(key: String) = {
		settings -= key
		self
	}
	
	def +=(kv: (String, Any)) = {
		settings += kv
		self
	}
	
	def ++=(other: T) = {
		settings ++= other.settings
		self
	}
	
	def iterator = settings.iterator
	def get(key: String) = settings.get(key)
	def getOrElse(key: String, default: => Any) = settings.getOrElse(key, default)
	def seq = settings.seq

}