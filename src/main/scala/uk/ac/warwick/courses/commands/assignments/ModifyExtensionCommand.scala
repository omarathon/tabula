package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands.Command
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.{ Autowired, Configurable }
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.helpers.{ LazyLists, Logging }
import org.springframework.transaction.annotation.Transactional
import reflect.BeanProperty
import org.joda.time.DateTime
import uk.ac.warwick.courses.{ DateFormats, CurrentUser }
import uk.ac.warwick.courses.services.UserLookupService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.Errors

/*
 * Built the command as a bulk operation. Single additions can be achieved by adding only one extension to the list.
 */

@Configurable
abstract class ModifyExtensionCommand extends Command[List[Extension]] with Daoisms with Logging {

	def submitter: CurrentUser
	def assignment: Assignment

	@Autowired var userLookup: UserLookupService = _
	@BeanProperty var extensionItems: JList[ExtensionItem] = LazyLists.simpleFactory()
	@BeanProperty var extensions: JList[Extension] = LazyLists.simpleFactory()

	@Transactional def copyExtensions(): List[Extension] = {

		def retrieveExtension(item: ExtensionItem) = {
			val extension = assignment.findExtension(item.universityId).getOrElse({
				val newExtension = new Extension
				/*
				 * For manually created extensions we must lookup the user code. When the student requests a extension we can
				 * capture this on creation
				 */
				newExtension.userId = userLookup.getUserByWarwickUniId(item.universityId).getUserId
				newExtension
			})
			extension.assignment = assignment
			extension.universityId = item.universityId
			extension.expiryDate = item.expiryDate
			extension.reason = item.reason
			extension.approved = true
			extension.approvedOn = DateTime.now
			extension
		}

		val extensionList = extensionItems map (retrieveExtension(_))
		extensionList.toList
	}

	@Transactional def persistExtensions() {
		extensions.foreach(session.saveOrUpdate(_))
	}

	def validate(errors: Errors) {
		if (extensionItems != null && !extensionItems.isEmpty) {
			for (i <- 0 until extensionItems.length) {
				val extension = extensionItems.get(i)
				errors.pushNestedPath("extensionItems[" + i + "]")
				validateExtension(extension, errors)
				errors.popNestedPath()
			}
		}
	}

	def validateExtension(extension: ExtensionItem, errors: Errors) {
		if (extension.expiryDate.isBefore(assignment.closeDate)) {
			errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
		}
	}

	@Transactional
	override def apply(): List[Extension] = {
		extensions = copyExtensions()
		persistExtensions()
		extensions.toList
	}

}

class ExtensionItem {

	@BeanProperty var universityId: String = _
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var expiryDate: DateTime = _
	@BeanProperty var reason: String = _

	def this(universityId: String, expiryDate: DateTime, reason: String) = {
		this()
		this.universityId = universityId
		this.expiryDate = expiryDate
		this.reason = reason
	}
}
