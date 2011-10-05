package uk.ac.warwick.courses.data.model
import scala.reflect.BeanProperty

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.GenericGenerator

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity @AccessType("field")
class Module {
	@BeanProperty
	@Id @GeneratedValue(generator="system-uuid") @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
	var id:String = null
	
	@BeanProperty var code:String = null
	@BeanProperty var name:String = null
	
}