package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.{MeetingFormat, StudentRelationshipType, HasSettings, GeneratedId}
import javax.persistence._
import javax.validation.constraints.NotNull
import org.joda.time.DateTime
import scala.Array
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.annotations.{Type, BatchSize}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.RelationshipService
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.PostLoadBehaviour

@Entity
class MonitoringPoint extends GeneratedId with HasSettings with PostLoadBehaviour {
	import MonitoringPoint._

	@ManyToOne
	@JoinColumn(name = "point_set_id")
	var pointSet: AbstractMonitoringPointSet = _

	@OneToMany(mappedBy = "point", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var checkpoints: JList[MonitoringCheckpoint] = JArrayList()
	
	@NotNull
	var name: String = _
	
	var createdDate: DateTime = _
	
	var updatedDate: DateTime = _

	@NotNull
	var validFromWeek: Int = _

	@NotNull
	var requiredFromWeek: Int = _

	def isLate(currentAcademicWeek: Int): Boolean = {
		currentAcademicWeek > requiredFromWeek
	}

	var sentToAcademicOffice: Boolean = false

	@Column(name="point_type")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.MonitoringPointTypeUserType")
	var pointType: MonitoringPointType = _

	@transient
	var relationshipService = Wire[RelationshipService]

	override def postLoad() {
		ensureSettings
	}

	// Setting for MonitoringPointType.Meeting
	def meetingRelationships = getStringSeqSetting(Settings.MeetingRelationships, Seq()).map(relationshipService.getStudentRelationshipTypeById(_).getOrElse(null))
	def meetingRelationships_= (relationships: Seq[StudentRelationshipType]):Unit =
		settings += (Settings.MeetingRelationships -> relationships.map(_.id))
	// Ugh. This sucks. But Spring always wants to use the Seq version if they share a method name, and therefore won't bind
	def meetingRelationshipsSpring_= (relationships: JSet[StudentRelationshipType]):Unit = {
		meetingRelationships = relationships.asScala.toSeq
	}

	def meetingFormats = getStringSeqSetting(Settings.MeetingFormats, Seq()).map(MeetingFormat.fromDescription(_))
	def meetingFormats_= (formats: Seq[MeetingFormat]) =
		settings += (Settings.MeetingFormats -> formats.map(_.description))
	// See above
	def meetingFormatsSpring_= (formats: JSet[MeetingFormat]) =
		meetingFormats = formats.asScala.toSeq

	def meetingQuantity = getIntSetting(Settings.MeetingQuantity, 1)
	def meetingQuantity_= (quantity: Int) = settings += (Settings.MeetingQuantity -> quantity)
}

object MonitoringPoint {

	object Settings {
		val MeetingRelationships = "meetingRelationships"
		val MeetingFormats = "meetingFormats"
		val MeetingQuantity = "meetingQuantity"
	}
}
