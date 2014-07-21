package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import javax.persistence.Lob
import scala.annotation.target.field

object HasSettings {
	/** Wrapper for get/put operations on a given string setting. */
	protected case class IntSetting(h: HasSettings)(key: String, default: Int) {
		def value = h.getIntSetting(key, default)
		def value_=(v: Int) { h.settings += key -> v }
	}

	/** Wrapper for get/put operations on a given string setting. */
	protected case class StringSetting(h: HasSettings)(key: String) {
		def value = h.getStringSetting(key)
		def value_=(v: String) { h.settings += key -> v }
	}

	protected case class StringMapSetting(h: HasSettings)(key: String) {
		def value = h.getStringMapSetting(key, Map())
		def value_=(v: Map[String,String]) { h.settings += key -> v }
	}
}

trait HasSettings {

	@Lob
	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	protected var settings: Map[String, Any] = Map()

	protected def settingsIterator = settings.iterator

	protected def getSetting(key: String) = Option(settings).flatMap { _.get(key) }

	protected def getStringSetting(key: String) = getSetting(key) match {
		case Some(value: String) => Some(value)
		case _ => None
	}
	protected def getIntSetting(key: String) = getSetting(key) match {
		case Some(value: Int) => Some(value)
		case _ => None
	}
	protected def getBooleanSetting(key: String) = getSetting(key) match {
		case Some(value: Boolean) => Some(value)
		case _ => None
	}
	protected def getStringSeqSetting(key: String) = getSetting(key) match {
		case Some(value: Seq[_]) => Some(value.asInstanceOf[Seq[String]])
		case _ => None
	}
	protected def getStringMapSetting(key: String) = {
		getSetting(key) match {
			case Some(value: Map[_, _]) => Some(value.asInstanceOf[Map[String, String]])
			case Some(value: collection.mutable.Map[_, _]) => Some(value.toMap.asInstanceOf[Map[String, String]])
			case _ => None
		}
	}

	def StringSetting = HasSettings.StringSetting(this) _
	def StringMapSetting = HasSettings.StringMapSetting(this) _
	def IntSetting = HasSettings.IntSetting(this) _

	protected def getStringSetting(key: String, default: => String): String = getStringSetting(key) getOrElse(default)
	protected def getIntSetting(key: String, default: => Int): Int = getIntSetting(key) getOrElse(default)
	protected def getBooleanSetting(key: String, default: => Boolean): Boolean = getBooleanSetting(key) getOrElse(default)
	protected def getStringSeqSetting(key: String, default: => Seq[String]): Seq[String] = getStringSeqSetting(key) getOrElse(default)
	protected def getStringMapSetting(key: String, default: => Map[String, String]): Map[String, String] = getStringMapSetting(key) getOrElse(default)

	protected def settingsSeq = Option(settings).map { _.toSeq }.getOrElse(Nil)

	protected def ensureSettings {
		if (settings == null) settings = Map()
	}
}

/**
 * Danger!
 *
 * Exposing the ++= methods publicly means that other classes can directly manipulate the keys of this map.
 * If your class expects keys to have certain specified values (e.g. enums) then mix in HasSettings and
 * expose type-safe getters and setters instead of mixing in SettingsMap directly
 */

trait SettingsMap extends HasSettings {

	protected def -=(key: String): this.type = {
		settings -= key
		this
	}
	
	protected def +=(kv: (String, Any)): this.type = {
		settings += kv
		this
	}

	protected def ++=(sets: Pair[String, Any]*): this.type = {
		settings ++= sets
		this
	}

	def ++=(other: SettingsMap): this.type = {
		this.settings ++= other.settings
		this
	}
}