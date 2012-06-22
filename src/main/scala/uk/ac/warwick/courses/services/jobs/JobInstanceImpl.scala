package uk.ac.warwick.courses.services.jobs

import scala.collection.mutable
import org.codehaus.jackson.map.ObjectMapper
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Lob
import uk.ac.warwick.courses.JavaImports.JList
import uk.ac.warwick.courses.data.PostLoadBehaviour
import uk.ac.warwick.courses.data.PreSaveBehaviour
import uk.ac.warwick.courses.data.model.GeneratedId
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.jobs.JobPrototype
import uk.ac.warwick.courses.helpers.Logging

object JobInstanceImpl {
	def fromPrototype(prototype: JobPrototype) = {
		val instance = new JobInstanceImpl
		instance.jobType = prototype.identifier
		instance.json = prototype.map
		instance
	}
}

/**
 * JobDefinition is the database entity that stores
 * data about the job request, its status and progress.
 * There can be many Job subclasses but JobDefinition
 * does not need subclassing. 
 */
@Configurable 
@Entity(name="Job")
class JobInstanceImpl() extends JobInstance with GeneratedId with PostLoadBehaviour with Logging {
	
	private type JsonMap = Map[String,Any]
	
	@transient @Autowired var jsonMapper:ObjectMapper =_
	
	/** Human-readable status of the job */
	var status:String =_
	
	var jobType:String =_
	
	var started = false
	var finished = false
	var succeeded = false
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate: DateTime = new DateTime
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var updatedDate: DateTime = new DateTime
	
	@Column(name="progress") var _progress:Int = 0
	def progress = _progress
	def progress_=(p: Int) = {
		_progress = p
	}
	
	@Lob var data:String = "{}"
	@transient private var _json: JsonMap = Map()
	def json = _json
	def json_=(map:JsonMap) {
		_json = map
		if (jsonMapper != null) {
			data = jsonMapper.writeValueAsString(json)
		} else {
			logger.warn("JSON mapper not set on JobInstanceImpl")
		}
	}

	def propsMap = json
	def propsMap_=(map:JsonMap) { json = map }
	
	override def postLoad {
		val map = jsonMapper.readValue(data, classOf[Map[String,Any]])
		json = map
	}
	
}