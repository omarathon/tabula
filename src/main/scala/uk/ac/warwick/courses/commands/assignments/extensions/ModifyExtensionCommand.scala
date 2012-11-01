package uk.ac.warwick.courses.commands.assignments.extensions

import uk.ac.warwick.courses.commands.{Description, Command}
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.helpers.{LazyLists, Logging}
import org.springframework.transaction.annotation.Transactional
import reflect.BeanProperty
import org.joda.time.DateTime
import uk.ac.warwick.courses.{DateFormats, CurrentUser}
import uk.ac.warwick.courses.services.UserLookupService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.Errors

/*
 * Built the command as a bulk operation. Single additions can be achieved by adding only one extension to the list.
 */

@Configurable
class ModifyExtensionCommand(val assignment:Assignment, val submitter: CurrentUser)
		extends Command[List[Extension]] with Daoisms with Logging	{
		
	@Autowired var userLookup:UserLookupService =_
	@BeanProperty var extensionItems:JList[ExtensionItem] = LazyLists.simpleFactory()
	@BeanProperty var extensions:JList[Extension] = LazyLists.simpleFactory()

	/**
	 * Transforms the commands extensionItems into Extension beans for persisting
	 */
	def copyExtensionItems(): List[Extension] = {
		def retrieveExtension(item:ExtensionItem) = {
			val extension = assignment.findExtension(item.universityId).getOrElse({
				val newExtension = new Extension(item.universityId)
				/*
				 * For manually created extensions we must lookup the user code. When the student requests a extension
				 * we can capture this on creation
				 */
				newExtension.userId = userLookup.getUserByWarwickUniId(item.universityId).getUserId
				newExtension
			})
			extension.assignment = assignment
			extension.expiryDate = item.expiryDate
			extension.approvalComments = item.approvalComments
			extension.approved = item.approved
			extension.rejected = item.rejected
			extension.approvedOn = DateTime.now
			extension
		}

		val extensionList = extensionItems map (retrieveExtension(_))
		extensionList.toList
	}

	/**
	 * Copies the specified extensions to the extensionItems array ready for editing
	 */
	def copyExtensions(extensions:List[Extension]){

		val extensionItemsList = for (extension <- extensions) yield {
			val item = new ExtensionItem
			item.universityId =  extension.universityId
			item.approvalComments = extension.approvalComments
			item.expiryDate = extension.expiryDate
			item
		}

		extensionItems.addAll(extensionItemsList)
	}

	@Transactional
	def persistExtensions() {
		extensions.foreach(session.saveOrUpdate(_))
	}

	def validate(errors:Errors) {
		if (extensionItems != null && !extensionItems.isEmpty) {
			for (i <- 0 until extensionItems.length) {
				val extension = extensionItems.get(i)
				errors.pushNestedPath("extensionItems["+i+"]")
				validateExtension(extension, errors)
				errors.popNestedPath()
			}
		}
	}

	def validateExtension(extension:ExtensionItem, errors:Errors){
		if(extension.expiryDate == null){
			if (!extension.rejected){
				errors.rejectValue("expiryDate", "extension.requestedExpiryDate.provideExpiry")
			}
		} else if(extension.expiryDate.isBefore(assignment.closeDate)){
			errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
		}
	}

	@Transactional
	override def work():List[Extension] = {
		extensions = copyExtensionItems()
		persistExtensions()
		extensions.toList
	}

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
		d.studentIds(extensionItems map (_.universityId))
	}
}

class ExtensionItem{

	@BeanProperty var universityId:String =_
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var expiryDate:DateTime =_
	@BeanProperty var approvalComments:String =_

	@BeanProperty var approved:Boolean = false
	@BeanProperty var rejected:Boolean = false

	def this(universityId:String, expiryDate:DateTime, reason:String) = {
		this()
		this.universityId = universityId
		this.expiryDate = expiryDate
		this.approvalComments = approvalComments
	}
}
