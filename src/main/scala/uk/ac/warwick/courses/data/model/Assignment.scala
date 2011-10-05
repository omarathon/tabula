package uk.ac.warwick.courses.data.model
import scala.reflect._
import javax.persistence.Id
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.AccessType

@Entity @AccessType("field")
class Assignment {
	@BeanProperty
	@Id @GeneratedValue(generator="system-uuid") @GenericGenerator(name="system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
	var id:String = null
	
	@BeanProperty 
	var name:String = null
}