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
import java.lang.Integer
import org.hibernate.annotations.Type

@Configurable
@Entity @Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="fieldtype")
abstract class FormField extends GeneratedId /*with PreSaveBehaviour with PostLoadBehaviour*/ {

	def this(a:Assignment) = {
		this()
		assignment = a
	}
	
	@Autowired @transient var json:ObjectMapper =_
	
//	var fieldType:String =_
	
	@BeanProperty
	@ManyToOne
	@(JoinColumn @field)(name="assignment_id", updatable=false, nullable=false)
	var assignment:Assignment =_
	
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
	
	@Type(`type`="int")
	@BeanProperty var position:Integer = 0
	
	@transient lazy val template = getClass.getAnnotation(classOf[DiscriminatorValue]).value
	
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

@Entity 
@DiscriminatorValue("comment")
class CommentField extends FormField with SimpleValue[String]  {
	override def isReadOnly = true
	
	
}

@Entity 
@DiscriminatorValue("text")
class TextField extends FormField  {
	
}

@Entity 
@DiscriminatorValue("textarea")
class TextareaField extends FormField {
	
}

@Entity 
@DiscriminatorValue("checkbox")
class CheckboxField extends FormField {
	
}

@Entity 
@DiscriminatorValue("file")
class FileField extends FormField {
	
}