package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.Logging

trait ExtensionService {
	def getExtensionById(id: String): Option[Extension]
	def countUnapprovedExtensions(assignment:Assignment): Int
}

@Service(value = "extensionService")
class ExtensionServiceImpl extends ExtensionService with Daoisms with Logging {

	def getExtensionById(id: String) = getById[Extension](id)

	def countUnapprovedExtensions(assignment:Assignment): Int = {
		session.createSQLQuery("""select count(*) from extension where assignment_id = :assignmentId
															and requestedon is not null and approved=0 and rejected=0""")
			.setString("assignmentId", assignment.id)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}
}