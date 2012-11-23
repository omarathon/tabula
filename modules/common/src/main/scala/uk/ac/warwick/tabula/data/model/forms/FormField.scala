package uk.ac.warwick.tabula.data.model.forms

import java.io.StringReader
import scala.annotation.target.field
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.codehaus.jackson.map.ObjectMapper
import org.hibernate.annotations.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.validation.Errors
import org.springframework.web.util.HtmlUtils._
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.Assignment

import uk.ac.warwick.tabula.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.data.FileDao
import org.springframework.beans.factory.annotation.Configurable
import scala.xml.NodeSeq
import uk.ac.warwick.tabula.helpers.ArrayList
import org.springframework.web.multipart.commons.CommonsMultipartFile
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.data.model.{MarkScheme, FileAttachment}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupService

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
@Entity @Access(AccessType.FIELD)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "fieldtype")
abstract class FormField extends GeneratedId {

	def this(a: Assignment) = {
		this()
		assignment = a
	}

	@transient var json = Wire.auto[ObjectMapper]
	@transient var userLookup = Wire.auto[UserLookupService]
	//	var fieldType:String =_

	@BeanProperty
	@ManyToOne
	@(JoinColumn @field)(name = "assignment_id", updatable = false, nullable = false)
	var assignment: Assignment = _

	@BeanProperty var name: String = _
	@BeanProperty var label: String = _
	@BeanProperty var instructions: String = _
	@BeanProperty var required: Boolean = _

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

	@transient @BeanProperty var propertiesMap: collection.Map[String, Any] = Map()

	protected def setProperty(name: String, value: Any) = {
		propertiesMap += name -> value
	}
	/**
	 * Fetch a property out of the property map if it matches the type.
	 * Careful with types as they are generally the ones that the JSON library
	 * has decided on, so integers come out as java.lang.Integer, and Int
	 * won't match.
	 */
	protected def getProperty[T](name: String, default: T)(implicit m: Manifest[T]) =
		propertiesMap.get(name) match {
			case Some(obj) if m.erasure.isInstance(obj) => obj.asInstanceOf[T]
			case _ => default
		}

	def isReadOnly = false
	final def readOnly = isReadOnly

	@Type(`type` = "int")
	@BeanProperty var position: JInteger = 0

	/** Determines which Freemarker template is used to render it. */
	@transient lazy val template = getClass.getAnnotation(classOf[DiscriminatorValue]).value

	/**
	 * Return a blank SubmissionValue that can be used to bind a submission
	 * of the same type as this FormField.
	 */
	def blankSubmissionValue: SubmissionValue

	def validate(value: SubmissionValue, errors: Errors)

}

trait SimpleValue[T] { self: FormField =>
	def value_=(value: T) { propertiesMap += "value" -> value }
	def setValue(value: T) = value_=(value)

	def value: T = propertiesMap.getOrElse("value", null).asInstanceOf[T]
	def getValue() = value

	def blankSubmissionValue = new StringSubmissionValue(this)
}

@Entity
@DiscriminatorValue("comment")
class CommentField extends FormField with SimpleValue[String] {
	override def isReadOnly = true

	/**
	 * Return a formatted version of the text that can be inserted
	 * WITHOUT escaping.
	 */
	def formattedHtml: String = Option(value) map { raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map { p => <p>{ p }</p> }
		(NodeSeq fromSeq nodes).toString
	} getOrElse ("")

	override def validate(value: SubmissionValue, errors: Errors) {}
}

@Entity
@DiscriminatorValue("text")
class TextField extends FormField with SimpleValue[String] {
	override def validate(value: SubmissionValue, errors: Errors) {}
}

@Entity
@DiscriminatorValue("wordcount")
class WordCountField extends FormField with SimpleValue[Int] {
	def min: JInteger = getProperty[JInteger]("min", null)
	def min_=(limit: JInteger) = setProperty("min", limit)
	def max: JInteger = getProperty[JInteger]("max", null)
	def max_=(limit: JInteger) = setProperty("max", limit)
	def conventions: String = getProperty[String]("conventions", null)
	def conventions_=(conventions: String) = setProperty("conventions", conventions)

	override def validate(value: SubmissionValue, errors: Errors) {
		value match {
			case i:StringSubmissionValue if !i.value.matches("\\d+") => errors.rejectValue("value", "assignment.submit.wordCount.missing")
			case i:StringSubmissionValue if (i.value.toInt < min || i.value.toInt > max) => errors.rejectValue("value", "assignment.submit.wordCount.outOfRange")
			case _ => // valid
		}
	}
}

@Entity
@DiscriminatorValue("textarea")
class TextareaField extends FormField with SimpleValue[String] {
	override def validate(value: SubmissionValue, errors: Errors) {}
}

@Entity
@DiscriminatorValue("checkbox")
class CheckboxField extends FormField {
	def blankSubmissionValue = new BooleanSubmissionValue(this)
	override def validate(value: SubmissionValue, errors: Errors) {}
}

@Entity
@DiscriminatorValue("marker")
class MarkerSelectField() extends FormField with SimpleValue[String] {

	def markers:Seq[User] = {
		if (assignment.markScheme == null) Seq()
		else assignment.markScheme.firstMarkers.includeUsers.map(userLookup.getUserByUserId(_))
	}

	override def validate(value: SubmissionValue, errors: Errors) {
		value match {
			case v: StringSubmissionValue => {
				Option(v.value) match {
					case None => errors.rejectValue("value", "marker.missing")
					case Some(v) if (v == "") => errors.rejectValue("value", "marker.missing")
					case _ =>
				}
			}
		}
	}
}

@Entity
@DiscriminatorValue("file")
class FileField extends FormField {
	def blankSubmissionValue = new FileSubmissionValue(this)

	def attachmentLimit: Int = getProperty[JInteger]("attachmentLimit", 1)
	def attachmentLimit_=(limit: Int) = setProperty("attachmentLimit", limit)

	// List of extensions.
	def attachmentTypes: Seq[String] = getProperty[JList[String]]("attachmentTypes", ArrayList())
	def attachmentTypes_=(types: Seq[String]) = setProperty("attachmentTypes", types: JList[String])
	
	// This is before onBind is called, so multipart files have not been persisted
	// as attachments yet.
	override def validate(value: SubmissionValue, errors: Errors) {
		
		/** Are there any duplicate values (ignoring case)? */
		def hasDuplicates(names: Seq[String]) = names.size != names.map(_.toLowerCase()).distinct.size
		
		value match {
			case v: FileSubmissionValue => {
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
