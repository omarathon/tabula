package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import uk.ac.warwick.tabula.commands.{Notifies, Description, Command, SelfValidating}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{StudentMember, Assignment, Module}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.{LazyLists, Logging}
import uk.ac.warwick.tabula.data.Transactions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.services.UserLookupService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications._
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.tabula.validators.WithinYears

/*
 * Built the command as a bulk operation. Single additions can be achieved by adding only one extension to the list.
 */

class AddExtensionCommand(module: Module, assignment: Assignment, submitter: CurrentUser)
	extends ModifyExtensionCommand(module, assignment, submitter) with Notifies[Seq[Extension], Option[Extension]] {
	
	PermissionCheck(Permissions.Extension.Create, assignment)

	def emit(extensions: Seq[Extension]) = extensions.map({extension =>
		val student = userLookup.getUserByWarwickUniId(extension.universityId)
		new ExtensionGrantedNotification(extension, student, submitter.apparentUser) with FreemarkerTextRenderer
	})
}

class EditExtensionCommand(module: Module, assignment: Assignment, val extension: Extension, submitter: CurrentUser)
	extends ModifyExtensionCommand(module, assignment, submitter) with Notifies[Seq[Extension], Option[Extension]] {
	
	PermissionCheck(Permissions.Extension.Update, extension)
	
	copyExtensions(List(extension))

	def emit(extensions: Seq[Extension]) = extensions.flatMap({extension =>
			val student = userLookup.getUserByWarwickUniId(extension.universityId)
			val admin = submitter.apparentUser

			val studentNotification = if(extension.isManual){
				new ExtensionChangedNotification(extension, student, admin) with FreemarkerTextRenderer
			} else if (extension.approved) {
				new ExtensionRequestApprovedNotification(extension, student, admin) with FreemarkerTextRenderer
			} else {
				new ExtensionRequestRejectedNotification(extension, student, admin) with FreemarkerTextRenderer
			}

			val adminNotifications = new ExtensionRequestRespondedNotification(extension, student, admin) with FreemarkerTextRenderer

		Seq(studentNotification, adminNotifications)
	})
}

class ReviewExtensionRequestCommand(module: Module, assignment: Assignment, extension: Extension, submitter: CurrentUser)
	extends EditExtensionCommand(module, assignment, extension, submitter) {

	PermissionCheck(Permissions.Extension.ReviewRequest, extension)
}

abstract class ModifyExtensionCommand(val module:Module, val assignment:Assignment, val submitter: CurrentUser)
		extends Command[Seq[Extension]] with Daoisms with Logging with SelfValidating {
	
	mustBeLinked(assignment,module)

	var userLookup = Wire.auto[UserLookupService]
	
	var extensionItems:JList[ExtensionItem] = LazyLists.create()
	var extensions:JList[Extension] = LazyLists.create()

	/**
	 * Transforms the commands extensionItems into Extension beans for persisting
	 */
	def copyExtensionItems(): Seq[Extension] = {
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
	def copyExtensions(extensions:Seq[Extension]){

		val extensionItemsList = for (extension <- extensions) yield {
			val item = new ExtensionItem
			item.universityId =  extension.universityId
			item.approvalComments = extension.approvalComments
			item.expiryDate = Option(extension.expiryDate).getOrElse(extension.requestedExpiryDate)
			item
		}

		extensionItems.addAll(extensionItemsList)
	}

	def persistExtensions() {
		transactional() {
			extensions.foreach(session.saveOrUpdate(_))
		}
	}

	def validate(errors:Errors) {
		if (extensionItems != null && !extensionItems.isEmpty) {
			for (i <- 0 until extensionItems.length) {
				val extension = extensionItems.get(i)
				errors.pushNestedPath("extensionItems[" + i + "]")
				validateExtension(extension, errors)
				errors.popNestedPath()
			}
		}
	}

	def validateExtension(extension:ExtensionItem, errors:Errors){

		val userId = userLookup.getUserByWarwickUniId(extension.universityId).getUserId
		if(userId == null) {
			errors.rejectValue("universityId", "extension.universityId.noValidUserId")
		}

		if(extension.expiryDate == null){
			if (!extension.rejected){
				errors.rejectValue("expiryDate", "extension.requestedExpiryDate.provideExpiry")
			}
		} else if(extension.expiryDate.isBefore(assignment.closeDate)){
			errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
		}
	}

	override def applyInternal():Seq[Extension] = transactional() {
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

	var universityId:String =_
	
	@WithinYears(maxFuture = 3) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var expiryDate:DateTime =_
	var approvalComments:String =_

	var approved:Boolean = false
	var rejected:Boolean = false

	def this(universityId:String, expiryDate:DateTime, reason:String) = {
		this()
		this.universityId = universityId
		this.expiryDate = expiryDate
		this.approvalComments = approvalComments
	}
}
