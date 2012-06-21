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

/**
 * JobDefinition is the database entity that stores
 * data about the job request, its status and progress.
 * There can be many Job subclasses but JobDefinition
 * does not need subclassing. 
 */
@Configurable 
@Entity
class JobInstanceImpl() extends JobInstance with GeneratedId with PreSaveBehaviour with PostLoadBehaviour {
	
	def this(prototype: JobPrototype) {
		this()
		jobType = prototype.identifier
		json ++= prototype.map
	}
	
	private type JsonMap = mutable.Map[String,Any]
	
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
	var finishedDate: DateTime = new DateTime
	
	@Column(name="progress") var _progress:Int = 0
	def progress = _progress
	def progress_=(p: Int) = {
		_progress = p
	}
	
	@Lob private var data:String = "{}"
	@transient var json: JsonMap = mutable.Map()

	override def propsMap = json
	
	override def preSave(newRecord:Boolean) { data = jsonMapper.writeValueAsString(json) }
	override def postLoad {
		val map = jsonMapper.readValue(data, classOf[Map[String,Any]])
		json = mutable.Map( map.toSeq : _* )
	}
	
}