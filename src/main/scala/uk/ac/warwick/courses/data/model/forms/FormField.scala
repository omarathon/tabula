package uk.ac.warwick.courses.data.model.forms

import java.io.StringReader
import scala.annotation.target.field
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.codehaus.jackson.map.ObjectMapper
import org.hibernate.annotations.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.validation.Errors
import javax.persistence._
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.commands.UploadedFile
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.GeneratedId
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import uk.ac.warwick.courses.data.FileDao
import org.springframework.beans.factory.annotation.Configurable
import scala.xml.NodeSeq

/**
 * A FormField defines a field to be displayed on an Assignment
 * when a student is making a submission.
 * 
 * Submissions are bound in the command as SubmissionValue items,
 * and if validation passes a Submission object is saved with a
 * collection of SavedSubmissionValue objects.
 * 
 * Although there can be many types of FormField, many of them
 * can use the same SubmissionValue class if they contain the
 * same sort of data (e.g. a string), and there is only one
 * SavedSubmissionValue class.
 * 
 */
@Configurable
@Entity @Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="fieldtype")
abstract class FormField extends GeneratedId {

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
	@BeanProperty var position:JInteger = 0
	
	/** Determines which Freemarker template is used to render it. */
	@transient lazy val template = getClass.getAnnotation(classOf[DiscriminatorValue]).value

	/**
	 * Return a blank SubmissionValue that can be used to bind a submission
	 * of the same type as this FormField.
	 */
	def blankSubmissionValue:SubmissionValue
	
	def validate(value:SubmissionValue, errors:Errors)
	
}



trait SimpleValue[T] { self:FormField =>
	def value_=(value:T) { propertiesMap += "value" -> value }
	def setValue(value:T) = value_=(value)
	
	def value:T = propertiesMap("value").asInstanceOf[T]
	def getValue() = value
	
	def blankSubmissionValue = new StringSubmissionValue(this)
}

@Entity 
@DiscriminatorValue("comment")
class CommentField extends FormField with SimpleValue[String]  {
	override def isReadOnly = true
	
	/**
	 * Return a formatted version of the text that can be inserted
	 * WITHOUT escaping.
	 */
	def formattedHtml:String = Option(value) map { raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map{ p => <p>{p}</p> }
		(NodeSeq fromSeq nodes).toString 
	} getOrElse("")
	
	override def validate(value:SubmissionValue, errors:Errors) {}
}

@Entity 
@DiscriminatorValue("text")
class TextField extends FormField with SimpleValue[String] {
	override def validate(value:SubmissionValue, errors:Errors) {}
}

@Entity 
@DiscriminatorValue("textarea")
class TextareaField extends FormField with SimpleValue[String] {
	override def validate(value:SubmissionValue, errors:Errors) {}
}

@Entity 
@DiscriminatorValue("checkbox")
class CheckboxField extends FormField {
	def blankSubmissionValue = new BooleanSubmissionValue(this)
	override def validate(value:SubmissionValue, errors:Errors) {}
}

@Entity 
@DiscriminatorValue("file")
class FileField extends FormField {
	def blankSubmissionValue = new FileSubmissionValue(this)
	
	override def validate(value:SubmissionValue, errors:Errors) {
		value match {
			case v:FileSubmissionValue => 
				if (value.asInstanceOf[FileSubmissionValue].file.isMissing) {
					errors.rejectValue("file", "file.missing")
				}
		}
	}
	
}