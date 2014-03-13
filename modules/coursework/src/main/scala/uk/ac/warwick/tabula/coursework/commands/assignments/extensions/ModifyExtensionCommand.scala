package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import uk.ac.warwick.tabula.commands.{Notifies, Description, Command, SelfValidating}
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.tabula.data.model.{Notification, Assignment, Module}
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
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.data.model.notifications.{ExtensionRequestRespondedRejectNotification, ExtensionRequestRespondedApproveNotification, ExtensionRequestRejectedNotification, ExtensionRequestApprovedNotification, ExtensionChangedNotification, ExtensionGrantedNotification}

/*
 * Built the command as a bulk operation. Single additions can be achieved by adding only one extension to the list.
 */

class AddExtensionCommand(module: Module, assignment: Assignment, submitter: CurrentUser, action: String)
	extends ModifyExtensionCommand(module, assignment, submitter, action) with Notifies[Seq[Extension], Option[Extension]] {

	PermissionCheck(Permissions.Extension.Create, assignment)

	def emit(extensions: Seq[Extension]) = extensions.map({extension =>
		Notification.init(new ExtensionGrantedNotification, submitter.apparentUser, Seq(extension), extension.assignment)
	})
}

class EditExtensionCommand(module: Module, assignment: Assignment, val extension: Extension, submitter: CurrentUser, action: String)
	extends ModifyExtensionCommand(module, assignment, submitter, action) with Notifies[Seq[Extension], Option[Extension]] {

	PermissionCheck(Permissions.Extension.Update, extension)

	copyExtensions(List(extension))

	def emit(extensions: Seq[Extension]) = extensions.flatMap({extension =>
			val admin = submitter.apparentUser

			val baseNotification = if (extension.isManual){
				new ExtensionChangedNotification
			} else if (extension.approved) {
				new ExtensionRequestApprovedNotification
			} else {
				new ExtensionRequestRejectedNotification
			}
			val studentNotification = Notification.init(baseNotification, admin, Seq(extension), extension.assignment)

			val baseAdminNotification = if (extension.approved) {
				new ExtensionRequestRespondedApproveNotification
			} else {
				new ExtensionRequestRespondedRejectNotification
			}
			val adminNotifications = Notification.init(baseAdminNotification, admin, Seq(extension), extension.assignment)

		Seq(studentNotification, adminNotifications)
	})
}

class ReviewExtensionRequestCommand(module: Module, assignment: Assignment, extension: Extension, submitter: CurrentUser, action: String)
	extends EditExtensionCommand(module, assignment, extension, submitter, action) {

	PermissionCheck(Permissions.Extension.ReviewRequest, extension)
}

abstract class ModifyExtensionCommand(val module:Module, val assignment:Assignment, val submitter: CurrentUser, val action: String)
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
			extension.rawState_=(item.state)
			extension.reviewedOn = DateTime.now
			action match {
				case "approve" => extension.approve(item.reviewerComments)
				case "reject" => extension.reject(item.reviewerComments)
				case _ =>
			}
			extension
		}

		val extensionList = extensionItems map (ex => retrieveExtension(ex))
		extensionList.toList
	}

	/**
	 * Copies the specified extensions to the extensionItems array ready for editing
	 */
	def copyExtensions(extensions:Seq[Extension]){

		val extensionItemsList = for (extension <- extensions) yield {
			val item = new ExtensionItem
			item.universityId =  extension.universityId
			item.reviewerComments = extension.reviewerComments
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
			if (extension.state == ExtensionState.Approved){
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

class ExtensionItem {

	var universityId: String =_

	@WithinYears(maxFuture = 3) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var expiryDate: DateTime =_
	var reviewerComments: String =_
	var state: ExtensionState = ExtensionState.Unreviewed

	def this(universityId: String, expiryDate: DateTime, reason: String) = {
		this()
		this.universityId = universityId
		this.expiryDate = expiryDate
		this.reviewerComments = reviewerComments
	}
}
