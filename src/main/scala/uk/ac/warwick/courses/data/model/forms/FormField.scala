package uk.ac.warwick.courses.data.model.forms
import java.io.StringReader

import scala.annotation.target.field
import scala.reflect.BeanProperty

import org.codehaus.jackson.map.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable

import javax.persistence._
import javax.validation.constraints.NotNull
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.GeneratedId

@Configurable
@Entity @Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="fieldtype")
abstract class FormField (
		
		@BeanProperty
		@(ManyToOne @field)
		@(JoinColumn @field)(name="assignment_id")
		var assignment:Assignment
		
	) extends GeneratedId /*with PreSaveBehaviour with PostLoadBehaviour*/ {

	private def this() = this(null)
	
	@Autowired @transient var json:ObjectMapper =_
	
	@BeanProperty var name:String =_
	@BeanProperty var label:String =_
	@BeanProperty var instructions:String =_
	@BeanProperty var required:Boolean =_
	
	@Basic(optional=false)
	@Access(AccessType.PROPERTY)
	def getProperties() = {
		// TODO cache the string value.
		json.writeValueAsString(propertiesMap)
	}
	def properties = getProperties
	
	def setProperties(props:String) {
		propertiesMap = json.readValue(new StringReader(props), classOf[Map[String,Any]])
	}
	
	@transient @BeanProperty var propertiesMap:collection.Map[String,Any] = Map()
	
	def isReadOnly = false
	final def readOnly = isReadOnly
	
	// list position
	@BeanProperty var position:Int =_
	
//	override def preSave(newRecord:Boolean) {
//		properties = json.writeValueAsString(properties)
//	}
//	
//	override def postLoad {
//		propertiesMap = json.readValue(new StringReader(propertiesString), classOf[Map[String,Any]])
//	}
}

trait SimpleValue[T] { self:FormField =>
	def value_=(value:T) { propertiesMap += "value" -> value }
	def setValue(value:T) = value_=(value)
	
	def value = propertiesMap("value")
	def getValue() = value
}

@DiscriminatorValue("comment")
class CommentField(assignment:Assignment) extends FormField(assignment) with SimpleValue[String]  {
	override def isReadOnly = true
	
	
}

@DiscriminatorValue("text")
class TextField(assignment:Assignment) extends FormField(assignment)  {
	
}

@DiscriminatorValue("textarea")
class TextareaField(assignment:Assignment) extends FormField(assignment) {
	
}

@DiscriminatorValue("checkbox")
class CheckboxField(assignment:Assignment) extends FormField(assignment) {
	
}

@DiscriminatorValue("file")
class FileField(assignment:Assignment) extends FormField(assignment) {
	
}