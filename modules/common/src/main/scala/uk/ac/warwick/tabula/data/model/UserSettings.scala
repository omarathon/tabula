package uk.ac.warwick.tabula.data.model

import reflect.BeanProperty
import javax.persistence._
import org.hibernate.annotations.AccessType
import scala.Array
import javax.persistence.Id
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._
import uk.ac.warwick.tabula.helpers.ArrayList

@Entity
class UserSettings extends GeneratedId  {
	
	@BeanProperty var userId: String = null
	@BeanProperty var data: String = null
	
	@Transient
	var fieldsMap: Map[String, Any] = null 
	
	def this(userId: String) = {
		this()
		this.userId = userId		
	}
	

			
	override def toString = "UserSettings [" + data + "]"
}
