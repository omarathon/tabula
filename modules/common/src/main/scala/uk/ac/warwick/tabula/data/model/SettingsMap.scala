package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import javax.persistence._
import scala.beans.BeanProperty

trait SettingsMap[A <: SettingsMap[A]] { self: A =>
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	@BeanProperty var settings: Map[String, Any] = Map()
	
	protected def -=(key: String) = {
		settings -= key
		self
	}
	
	protected def +=(kv: (String, Any)) = {
		settings += kv
		self
	}
	
	def ++=(sets: Pair[String, Any]*) = {
		settings ++= sets
		self
	}
	
	def ++=(other: A) = {
		settings ++= other.settings
		self
	}
	
	protected def settingsIterator = settings.iterator
	
	protected def getSetting(key: String) = settings.get(key)
	
	protected def getStringSetting(key: String) = settings.get(key) match {
		case Some(value: String) => Some(value)
		case _ => None
	}
	protected def getIntSetting(key: String) = settings.get(key) match {
		case Some(value: Int) => Some(value)
		case _ => None
	}
	protected def getBooleanSetting(key: String) = settings.get(key) match {
		case Some(value: Boolean) => Some(value)
		case _ => None
	}
	
	protected def getStringSetting(key: String, default: => String): String = getStringSetting(key) getOrElse(default)
	protected def getIntSetting(key: String, default: => Int): Int = getIntSetting(key) getOrElse(default)
	protected def getBooleanSetting(key: String, default: => Boolean): Boolean = getBooleanSetting(key) getOrElse(default)
	
	protected def settingsSeq = settings.toSeq
	
	protected def ensureSettings {
		if (settings == null) settings = Map()
	}

}