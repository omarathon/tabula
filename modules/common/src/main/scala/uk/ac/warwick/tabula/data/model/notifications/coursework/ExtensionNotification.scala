package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, NotificationWithTarget, SingleItemNotification}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


abstract class ExtensionNotification extends NotificationWithTarget[Extension, Assignment]
	with SingleItemNotification[Extension]
	with AutowiringUserLookupComponent {

	def extension = item.entity
	def assignment = target.entity
	def student = userLookup.getUserByUserId(extension.userId)

	def titlePrefix = target.entity.module.code.toUpperCase + ": "
}
