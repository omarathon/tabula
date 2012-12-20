package uk.ac.warwick.tabula.services

import scala.Option.option2Iterable
import org.apache.lucene.search.ScoreDoc
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Activity
import uk.ac.warwick.tabula.data.model.Module
import org.apache.lucene.search.FieldDoc

/** At the moment, this uses AuditEvents as a proxy for things of interest,
 *  and specifically is only noticing new submission events.
 *  In the future it'll likely make sense to serve events of interest at whichever
 *  depth of Tabula we're looking from, and look directly at typed Activity[T] rather
 *  than just Activity[AuditEvent]
 * */
@Service
class ActivityService {
	
	private val StreamSize = 8
	
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	var assignmentService = Wire.auto[AssignmentService]
	var auditIndexService = Wire.auto[AuditEventIndexService]

	/** wrapper class to turn ScoreDoc plus searcher id token into a
	 *  tuple of simple values ready to pass as parameters
	 */  
	class PagedActivities(val activities: Seq[Activity[Any]], val doc: Option[FieldDoc], val token: Long, val total: Int) {
		def getTokens: String = doc match {
			case None => "empty"
			case _ => doc.get.doc + "/" + doc.get.fields(0) + "/" + token
		}
	}

	// first page
	def getNoteworthySubmissions(user: CurrentUser): PagedActivities = {
		val events = auditIndexService.noteworthySubmissionsForModules(getModules(user), None, None, StreamSize)
		
		new PagedActivities(events.docs flatMap (event => Activity(event)), events.last, events.token, events.total)
	}
	
	// following pages
	def getNoteworthySubmissions(user: CurrentUser, doc: Int, field: Long, token: Long): PagedActivities = {
		// slightly ugly implicit cast required, as we need a FieldDoc whose constructor expects a java Object[]
		val scoreDoc = new FieldDoc(doc, Float.NaN, Array(field:java.lang.Long))
		val events = auditIndexService.noteworthySubmissionsForModules(getModules(user), Option(scoreDoc), Option(token), StreamSize)
		
		new PagedActivities(events.docs flatMap (event => Activity(event)), events.last, events.token, events.total)
	}
	
	private def getModules(user: CurrentUser): Seq[Module] = {
		val ownedModules = moduleService.modulesManagedBy(user.idForPermissions).toSet
		val adminModules = moduleService.modulesAdministratedBy(user.idForPermissions).toSet
		
		(ownedModules ++ adminModules).toSeq
	}
}