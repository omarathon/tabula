package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.{ExtensionDao, FeedbackDao, Daoisms}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.Logging
import org.hibernate.criterion.{Projections, Restrictions}
import org.springframework.beans.factory.annotation.Autowired

trait ExtensionService {
	def getExtensionById(id: String): Option[Extension]
	def countExtensions(assignment: Assignment): Int
	def hasExtensions(assignment: Assignment): Boolean
	def countUnapprovedExtensions(assignment: Assignment): Int
	def hasUnapprovedExtensions(assignment: Assignment): Boolean
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]
}

@Service(value = "extensionService")
class ExtensionServiceImpl extends ExtensionService {

	@Autowired var dao: ExtensionDao = _

	def getExtensionById(id: String) = dao.getExtensionById(id)

	def countExtensions(assignment: Assignment): Int = dao.countExtensions(assignment)
	def hasExtensions(assignment: Assignment): Boolean = countExtensions(assignment) > 0

	def countUnapprovedExtensions(assignment: Assignment): Int = dao.countUnapprovedExtensions(assignment)
	def hasUnapprovedExtensions(assignment: Assignment): Boolean = countUnapprovedExtensions(assignment) > 0

	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = dao.getUnapprovedExtensions(assignment)
}