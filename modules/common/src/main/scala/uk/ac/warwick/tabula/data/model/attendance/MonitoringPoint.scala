package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.{Module, MeetingFormat, StudentRelationshipType, HasSettings, GeneratedId}
import javax.persistence._
import javax.validation.constraints.NotNull
import org.joda.time.DateTime
import scala.Array
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.annotations.{Type, BatchSize}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, RelationshipService}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.PostLoadBehaviour

@Entity
class MonitoringPoint extends CommonMonitoringPointProperties with MonitoringPointSettings {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_set_id")
	var pointSet: MonitoringPointSet = _

	@OneToMany(mappedBy = "point", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var checkpoints: JList[MonitoringCheckpoint] = JArrayList()

	def isLate(currentAcademicWeek: Int): Boolean = {
		currentAcademicWeek > requiredFromWeek
	}

	var sentToAcademicOffice: Boolean = false
}

trait CommonMonitoringPointProperties extends GeneratedId {
	@NotNull
	var name: String = _

	var createdDate: DateTime = _

	var updatedDate: DateTime = _

	@NotNull
	var validFromWeek: Int = _

	@NotNull
	var requiredFromWeek: Int = _

	@Column(name="point_type")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.MonitoringPointTypeUserType")
	var pointType: MonitoringPointType = _

	def toPoint() = {
		val point = new MonitoringPoint
		point.createdDate = new DateTime()
		point.updatedDate = new DateTime()
		point.name = name
		point.validFromWeek = validFromWeek
		point.requiredFromWeek = requiredFromWeek
		point.pointType = pointType
		point
	}
}

trait MonitoringPointSettings extends HasSettings with PostLoadBehaviour {
	import MonitoringPoint._

	@transient
	var relationshipService = Wire[RelationshipService]

	@transient
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

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

	def meetingFormats = getStringSeqSetting(Settings.MeetingFormats, Seq()).map(MeetingFormat.fromDescription)
	def meetingFormats_= (formats: Seq[MeetingFormat]) =
		settings += (Settings.MeetingFormats -> formats.map(_.description))
	// See above
	def meetingFormatsSpring_= (formats: JSet[MeetingFormat]) =
		meetingFormats = formats.asScala.toSeq

	def meetingQuantity = getIntSetting(Settings.MeetingQuantity, 1)
	def meetingQuantity_= (quantity: Int) = settings += (Settings.MeetingQuantity -> quantity)

	// Setting for MonitoringPointType.SmallGroup

	def smallGroupEventQuantity = getIntSetting(Settings.SmallGroupEventQuantity, 0)
	def smallGroupEventQuantity_= (quantity: Int): Unit = settings += (Settings.SmallGroupEventQuantity -> quantity)
	def smallGroupEventQuantity_= (quantity: JInteger): Unit = {
		smallGroupEventQuantity = quantity match {
			case q: JInteger => q.intValue
			case _ => 0
		}
	}

	def smallGroupEventModules = getStringSeqSetting(Settings.SmallGroupEventModules, Seq()).map(moduleAndDepartmentService.getModuleById(_).getOrElse(null))
	def smallGroupEventModules_= (modules: Seq[Module]) =
		settings += (Settings.SmallGroupEventModules -> modules.map(_.id))
	// See above
	def smallGroupEventModulesSpring_= (modules: JSet[Module]) =
		smallGroupEventModules = modules.asScala.toSeq
}

object MonitoringPoint {

	object Settings {
		val MeetingRelationships = "meetingRelationships"
		val MeetingFormats = "meetingFormats"
		val MeetingQuantity = "meetingQuantity"

		val SmallGroupEventQuantity = "smallGroupEventQuantity"
		val SmallGroupEventModules = "smallGroupEventModules"
	}
}
