package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.Logging
import org.hibernate.criterion.{Projections, Restrictions}

trait ExtensionService {
	def getExtensionById(id: String): Option[Extension]
	def countUnapprovedExtensions(assignment: Assignment): Int
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]
}

@Service(value = "extensionService")
class ExtensionServiceImpl extends ExtensionService with Daoisms with Logging {

	def getExtensionById(id: String) = getById[Extension](id)

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

	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = {
		unapprovedExtensionsCriteria(assignment).seq
	}
}