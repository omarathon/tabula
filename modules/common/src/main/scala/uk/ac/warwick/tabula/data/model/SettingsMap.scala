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
	
	def ++=(sets: Pair[String, Any]*) = {
		settings ++= sets
		self
	}
	
	def ++=(other: T) = {
		settings ++= other.settings
		self
	}
	
	def settingsIterator = settings.iterator
	
	def getSetting(key: String) = settings.get(key)
	
	def getStringSetting(key: String) = settings.get(key) match {
		case Some(value: String) => Some(value)
		case _ => None
	}
	def getIntSetting(key: String) = settings.get(key) match {
		case Some(value: Int) => Some(value)
		case _ => None
	}
	def getBooleanSetting(key: String) = settings.get(key) match {
		case Some(value: Boolean) => Some(value)
		case _ => None
	}
	
	def getStringSetting(key: String, default: => String): String = getStringSetting(key) getOrElse(default)
	def getIntSetting(key: String, default: => Int): Int = getIntSetting(key) getOrElse(default)
	def getBooleanSetting(key: String, default: => Boolean): Boolean = getBooleanSetting(key) getOrElse(default)
	
	def settingsSeq = settings.seq
	
	def ensureSettings {
		if (settings == null) settings = Map()
	}

}