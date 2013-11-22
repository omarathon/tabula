package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.spring.Wire

trait ExtensionServiceComponent {
	def extensionService: ExtensionService
}

trait AutowiringExtensionServiceComponent extends ExtensionServiceComponent {
	var extensionService = Wire[ExtensionService]
}
trait ExtensionService {
	def getExtensionById(id: String): Option[Extension]
	def countExtensions(assignment: Assignment): Int
	def hasExtensions(assignment: Assignment): Boolean
	def countUnapprovedExtensions(assignment: Assignment): Int
	def hasUnapprovedExtensions(assignment: Assignment): Boolean
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]
}

abstract class AbstractExtensionService extends ExtensionService {
	self: ExtensionDaoComponent =>

	def getExtensionById(id: String) = extensionDao.getExtensionById(id)
	def countExtensions(assignment: Assignment): Int = extensionDao.countExtensions(assignment)
	def hasExtensions(assignment: Assignment): Boolean = countExtensions(assignment) > 0
	def countUnapprovedExtensions(assignment: Assignment): Int = extensionDao.countUnapprovedExtensions(assignment)
	def hasUnapprovedExtensions(assignment: Assignment): Boolean = countUnapprovedExtensions(assignment) > 0
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = extensionDao.getUnapprovedExtensions(assignment)
}

@Service(value = "extensionService")
class ExtensionServiceImpl
	extends AbstractExtensionService
	with AutowiringExtensionDaoComponent