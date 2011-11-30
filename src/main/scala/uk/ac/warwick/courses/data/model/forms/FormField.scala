package uk.ac.warwick.courses.data.model.forms
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.GeneratedId
import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import org.springframework.validation.Validator
import org.springframework.validation.Errors
import scala.annotation.target.field
import scala.reflect.BeanProperty
import collection.mutable
import org.hibernate.annotations.ForceDiscriminator
import org.hibernate.annotations.DiscriminatorOptions

@Entity @AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="fieldtype")
//@DiscriminatorOptions(force=false)
abstract class FormField(
		
		@BeanProperty
		@(ManyToOne @field)
		@(JoinColumn @field)(name="assignment_id")
		var assignment:Assignment
		
	) extends GeneratedId {

	private def this() = this(null)
	
	@BeanProperty var name:String =_
	@BeanProperty var label:String =_
	@BeanProperty var instructions:String =_
	@BeanProperty var required:Boolean =_
	
	@Column(name="properties")
	@BeanProperty var propertiesString:String =_
	@transient @BeanProperty var properties:collection.Map[String,Any] = mutable.Map()
	
	def isReadOnly = false
	final def readOnly = isReadOnly
	
	// list position
	@BeanProperty var position:Int =_
	
}

trait SimpleValue[T] { self:FormField =>
	def value_=(value:T) { properties += "value" -> value }
	def setValue(value:T) = value_=(value)
	
	def value = properties("value")
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

