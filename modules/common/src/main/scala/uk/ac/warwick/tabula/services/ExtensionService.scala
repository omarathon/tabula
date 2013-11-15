package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.Logging
import org.hibernate.criterion.{Projections, Restrictions}

trait ExtensionService {
	def getExtensionById(id: String): Option[Extension]
	def countExtensions(assignment: Assignment): Int
	def hasExtensions(assignment: Assignment): Boolean
	def countUnapprovedExtensions(assignment: Assignment): Int
	def hasUnapprovedExtensions(assignment: Assignment): Boolean
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]
}

@Service(value = "extensionService")
class ExtensionServiceImpl extends ExtensionService with Daoisms with Logging {

	def getExtensionById(id: String) = getById[Extension](id)

	def countExtensions(assignment: Assignment): Int = {
		session.newCriteria[Extension]
			.add(is("assignment", assignment))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()
	}

	def hasExtensions(assignment: Assignment): Boolean = countExtensions(assignment) > 0

	private def unapprovedExtensionsCriteria(assignment: Assignment) = session.newCriteria[Extension]
	.add(is("assignment", assignment))
	.add(
		Restrictions.and(
			Restrictions.isNotNull("requestedOn"),
			Restrictions.eq("approved", false),
			Restrictions.eq("rejected", false)
		)
	)

	def countUnapprovedExtensions(assignment: Assignment): Int = {
		unapprovedExtensionsCriteria(assignment)
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()
	}

	def hasUnapprovedExtensions(assignment: Assignment): Boolean = countUnapprovedExtensions(assignment) > 0

	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = {
		unapprovedExtensionsCriteria(assignment).seq
	}
}