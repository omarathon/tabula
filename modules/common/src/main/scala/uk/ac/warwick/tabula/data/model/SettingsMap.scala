package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.annotations.Type
import scala.reflect.ClassTag

trait SettingsMap[A <: SettingsMap[A]] { self: A =>
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	var settings: Map[String, Any] = Map()

	/**
	 * Provides access to a setting via a `value` property.
	 *
	 * @param key the key where the setting is stored in the map.
	 * @param default default value if none is already set.
	 * @tparam A
	 */
	case class Setting[A : ClassTag](key: String, default: A) {
		def value: A = settings.get(key) map { _.asInstanceOf[A] } getOrElse(default)
		def value_=(value: A) {
			settings += (key -> value)
		}
	}

	case class OptionSetting[A : ClassTag](key: String) {
		def value: Option[A] = settings.get(key) map { _.asInstanceOf[A] }
		def value_=(value: A) {
			settings += (key -> Option(value))
		}
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
	protected def getStringSeqSetting(key: String) = settings.get(key) match {
		case Some(value: Seq[_]) => Some(value.asInstanceOf[Seq[String]])
		case _ => None
	}
	
	protected def getStringSetting(key: String, default: => String): String = getStringSetting(key) getOrElse(default)
	protected def getIntSetting(key: String, default: => Int): Int = getIntSetting(key) getOrElse(default)
	protected def getBooleanSetting(key: String, default: => Boolean): Boolean = getBooleanSetting(key) getOrElse(default)
	protected def getStringSeqSetting(key: String, default: => Seq[String]): Seq[String] = getStringSeqSetting(key) getOrElse(default)
	
	protected def settingsSeq = settings.toSeq
	
	protected def ensureSettings {
		if (settings == null) settings = Map()
	}

}