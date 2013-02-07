package uk.ac.warwick.tabula.data.model

import scala.collection._
import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import org.hibernate.annotations.Type
import javax.persistence._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class UserSettings extends GeneratedId with PermissionsTarget {
	
	@BeanProperty var userId: String = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	@Column(name="data")
	@BeanProperty var data: Map[String, Any] = Map()
		
	def this(userId: String) = {
		this()
		this.userId = userId		
	}
			
	override def toString = "UserSettings [" + data + "]"
	
	def permissionsParents = Seq()
	
	def -=(key: String) = {
		data -= key
		this
	}
	
	def +=(kv: (String, Any)) = {
		data += kv
		this
	}
	
	def ++=(other: UserSettings) = {
		data ++= other.data
		this
	}
	
	def iterator = data.iterator
	def get(key: String) = data.get(key)
	def getOrElse(key: String, default: => Any) = data.getOrElse(key, default)
	def seq = data.seq
	def empty = new UserSettings
	
}
