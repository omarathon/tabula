package uk.ac.warwick.tabula.data.model.forms

import java.io.StringReader
import scala.annotation.target.field
import collection.JavaConversions._
import org.hibernate.annotations.Type
import org.springframework.validation.Errors
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{ AbstractBasicUserType, GeneratedId}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupService
import scala.reflect._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import uk.ac.warwick.tabula.data.model.Gender.{Unspecified, Female, Male}

/**
 * A FormField defines a field to be displayed on an Assignment
 * when a student is making a submission or a marker is entering feedback.
 *
 * Submissions are bound in the command as FormValue items,
 * and if validation passes a Submission object is saved with a
 * collection of SavedFormValue objects.
 *
 * Although there can be many types of FormField, many of them
 * can use the same FormValue class if they contain the
 * same sort of data (e.g. a string), and there is only one
 * SavedFormValue class.
 *
 */

object FormField {
	final val FormFieldMaxSize = 4000
}

@Entity @Access(AccessType.FIELD)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "fieldtype")
abstract class FormField extends GeneratedId with Logging {

	@transient var json = JsonObjectMapperFactory.instance
	@transient var userLookup = Wire.auto[UserLookupService]

	@BeanProperty
	@ManyToOne
	@(JoinColumn @field)(name = "assignment_id", updatable = false, nullable = false)
	var assignment: Assignment = _

	var name: String = _
	var label: String = _
	var instructions: String = _
	var required: Boolean = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.forms.FormFieldContextUserType")
	var context: FormFieldContext = _

	@Basic(optional = false)
	@Access(AccessType.PROPERTY)
	def getProperties() = {
		// TODO cache the string value.
		json.writeValueAsString(propertiesMap)
	}
	def properties = getProperties

	def setProperties(props: String) {
		propertiesMap = json.readValue(new StringReader(props), classOf[Map[String, Any]])
	}

	@transient var propertiesMap: collection.Map[String, Any] = Map()

	protected def setProperty(name: String, value: Any) = {
		propertiesMap += name -> value
	}
	/**
	 * Fetch a property out of the property map if it matches the type.
	 * Careful with types as they are generally the ones that the JSON library
	 * has decided on, so integers come out as JInteger, and Int
	 * won't match.
	 */
	protected def getProperty[A : ClassTag](name: String, default: A) =
		propertiesMap.get(name) match {
			case Some(null) => default
			case Some(obj) if classTag[A].runtimeClass.isInstance(obj) => obj.asInstanceOf[A]
			case Some(obj) => {
				// TAB-705 warn when we return an unexpected type
				logger.warn("Expected property %s of type %s, but was %s".format(name, classTag[A].runtimeClass.getName, obj.getClass.getName))
				default
			}
			case _ => default
		}

	def isReadOnly = false
	final def readOnly = isReadOnly

	@Type(`type` = "int")
	var position: JInteger = 0

	/** Determines which Freemarker template is used to render it. */
	@transient lazy val template = getClass.getAnnotation(classOf[DiscriminatorValue]).value

	/**
	 * Return a blank FormValue that can be used to bind a submission
	 * of the same type as this FormField.
	 */
	def blankFormValue: FormValue

	/**
	 * Return a form value of the appropriate type with its value set
	 */
	def populatedFormValue(value: SavedFormValue) : FormValue

	def validate(value: FormValue, errors: Errors)

}

trait SimpleValue[A] { self: FormField =>
	def value_=(value: A) { propertiesMap += "value" -> value }
	def setValue(value: A) = value_=(value)

	def value: A = propertiesMap.getOrElse("value", null).asInstanceOf[A]
	def getValue() = value

	override def validate(value: FormValue, errors: Errors) {
		value match {
			case s: StringFormValue if s.value != null => {
				val length = s.value.toString.length
				if (length > FormField.FormFieldMaxSize)
					errors.rejectValue("value", "textfield.tooLarge", Array[Object](length: JInteger, FormField.FormFieldMaxSize: JInteger), "")
			}
			case _ =>
		}
	}

	def blankFormValue = new StringFormValue(this)

	def populatedFormValue(savedFormValue: SavedFormValue) = {
		val formValue = new StringFormValue(this)
		formValue.value = savedFormValue.value
		formValue
	}

}

@Entity
@DiscriminatorValue("comment")
class CommentField extends FormField with SimpleValue[String] with FormattedHtml {
	override def isReadOnly = true

	def formattedHtml: String = formattedHtml(Option(value))
}

@Entity
@DiscriminatorValue("text")
class TextField extends FormField with SimpleValue[String] {
}

@Entity
@DiscriminatorValue("wordcount")
class WordCountField extends FormField {
	context = FormFieldContext.Submission

	def min: JInteger = getProperty[JInteger]("min", null)
	def min_=(limit: JInteger) = setProperty("min", limit)
	def max: JInteger = getProperty[JInteger]("max", null)
	def max_=(limit: JInteger) = setProperty("max", limit)
	def conventions: String = getProperty[String]("conventions", null)
	def conventions_=(conventions: String) = setProperty("conventions", conventions)

	def blankFormValue = new IntegerFormValue(this)
	def populatedFormValue(savedFormValue: SavedFormValue) = {
		val formValue = new IntegerFormValue(this)
		formValue.value = savedFormValue.value.asInstanceOf[Integer]
		formValue
	}

	override def validate(value: FormValue, errors: Errors) {
		value match {
			case i:IntegerFormValue => {
				 if (i.value == null) errors.rejectValue("value", "assignment.submit.wordCount.missing")
				 else if (i.value < min || i.value > max) errors.rejectValue("value", "assignment.submit.wordCount.outOfRange")
			}
			case _ => errors.rejectValue("value", "assignment.submit.wordCount.missing") // value was null or wrong type
		}
	}
}

@Entity
@DiscriminatorValue("textarea")
class TextareaField extends FormField with SimpleValue[String] {}

@Entity
@DiscriminatorValue("checkbox")
class CheckboxField extends FormField {
	def blankFormValue = new BooleanFormValue(this)
	def populatedFormValue(savedFormValue: SavedFormValue) = {
		val formValue = new BooleanFormValue(this)
		formValue.value = savedFormValue.value.asInstanceOf[Boolean]
		formValue
	}
	override def validate(value: FormValue, errors: Errors) {}
}

@Entity
@DiscriminatorValue("marker")
class MarkerSelectField extends FormField with SimpleValue[String] {
	context = FormFieldContext.Submission

	def markers:Seq[User] = {
		if (assignment.markingWorkflow == null) Seq()
		else assignment.markingWorkflow.firstMarkers.includeUsers.map(userLookup.getUserByUserId(_))
	}

	override def validate(value: FormValue, errors: Errors) {
		super.validate(value, errors)
		value match {
			case v: StringFormValue => {
				Option(v.value) match {
					case None => errors.rejectValue("value", "marker.missing")
					case Some(v) if v == "" => errors.rejectValue("value", "marker.missing")
					case Some(v) if !markers.exists { _.getUserId == v } => errors.rejectValue("value", "marker.invalid")
					case _ =>
				}
			}
		}
	}
}

@Entity
@DiscriminatorValue("file")
class FileField extends FormField {
	def blankFormValue = new FileFormValue(this)
	def populatedFormValue(savedFormValue: SavedFormValue) = blankFormValue

	def attachmentLimit: Int = getProperty[JInteger]("attachmentLimit", 1)
	def attachmentLimit_=(limit: Int) = setProperty("attachmentLimit", limit)

	// List of extensions.
	def attachmentTypes: Seq[String] = getProperty[Seq[String]]("attachmentTypes", Seq())
	def attachmentTypes_=(types: Seq[String]) = setProperty("attachmentTypes", types: Seq[String])

	// This is after onBind is called, so any multipart files have been persisted as attachments
	override def validate(value: FormValue, errors: Errors) {

		/** Are there any duplicate values (ignoring case)? */
		def hasDuplicates(names: Seq[String]) = names.size != names.map(_.toLowerCase()).distinct.size

		value match {
			case v: FileFormValue => {
				if (v.file.isMissing) {
					errors.rejectValue("file", "file.missing")
				} else if (v.file.size > attachmentLimit) {
					if (attachmentLimit == 1) errors.rejectValue("file", "file.toomany.one")
					else errors.rejectValue("file", "file.toomany", Array(attachmentLimit: JInteger), "")
				} else if (hasDuplicates(v.file.fileNames)) {
					errors.rejectValue("file", "file.duplicate")
				} else if (!attachmentTypes.isEmpty) {
					val attachmentStrings = attachmentTypes.map(s => "." + s)
					val fileNames = v.file.fileNames map (_.toLowerCase)
					val invalidFiles = fileNames.filter(s => !attachmentStrings.exists(s.endsWith))
					if (invalidFiles.size > 0) {
						if (invalidFiles.size == 1) errors.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
						else errors.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
					}
				}
			}
		}
	}
}

sealed abstract class FormFieldContext(val dbValue: String, val description: String)

object FormFieldContext {
	case object Submission extends FormFieldContext("submission", "Submission")
	case object Feedback extends FormFieldContext("feedback", "Feedback")

	def fromCode(code: String) = code match {
		case Submission.dbValue => Submission
		case Feedback.dbValue => Feedback
		case _ => throw new IllegalArgumentException()
	}
}

class FormFieldContextUserType extends AbstractBasicUserType[FormFieldContext, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = FormFieldContext.fromCode(string)

	override def convertToValue(context: FormFieldContext) = context.dbValue

}
