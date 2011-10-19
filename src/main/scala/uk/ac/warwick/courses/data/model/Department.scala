package uk.ac.warwick.courses.data.model
import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.GenericGenerator
import javax.persistence._
import javax.validation.constraints._
import collection.JavaConversions._

@Entity @AccessType("field")
class Department extends GeneratedId {
  
	@BeanProperty var code:String = null
	
	@BeanProperty var name:String = null
	
	@OneToMany(mappedBy="department")
	@BeanProperty var modules:java.util.List[Module] = List()
	
	override def toString = "Department("+code+")"
	
}