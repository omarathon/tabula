package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence.{DiscriminatorType, OneToMany, _}

import org.hibernate.annotations.{BatchSize, Type}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, UserGroupCacheManager}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage._


@Entity
@Table(name="StageMarkers")
@Access(AccessType.FIELD)
class StageMarkers extends GeneratedId with Serializable {

	def this(stage: MarkingWorkflowStage, workflow: CM2MarkingWorkflow){
		this()
		this.workflow = workflow
		this.stage = stage
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStageUserType")
	var stage: MarkingWorkflowStage = _

	@ManyToOne(optional = false, cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	var workflow: CM2MarkingWorkflow = null

	@transient
	var cm2MarkingWorkflowService: Option[CM2MarkingWorkflowService] = Wire.option[CM2MarkingWorkflowService]

	/** The group of markers for this stage */
	@OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "markers")
	private var _markers = UserGroup.ofUsercodes
	def markers: UnspecifiedTypeUserGroup = {
		cm2MarkingWorkflowService match {
			case Some(service) =>
				new UserGroupCacheManager(_markers, service.markerHelper)
			case _ => _markers
		}
	}
	def markers_=(group: UserGroup) { _markers = group }

}

@Entity
@Table(name="MarkingWorkflow")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="WorkflowType", discriminatorType = DiscriminatorType.STRING, length=255)
@Access(AccessType.FIELD)
abstract class CM2MarkingWorkflow extends GeneratedId with PermissionsTarget with Serializable {

	type Usercode = String
	type UniversityId = String
	type Marker = User

	/** A descriptive name for the users' reference. */
	@Basic(optional = false)
	var name: String = null

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var department: Department = null

	@OneToMany(mappedBy = "cm2MarkingWorkflow", fetch = LAZY)
	@BatchSize(size = 200)
	var assignments: JList[Assignment] = JArrayList()

	@OneToMany(mappedBy = "workflow", fetch = LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var stageMarkers: JList[StageMarkers] = JArrayList()

	def allStages: Seq[MarkingWorkflowStage]
	def initialStages: Seq[MarkingWorkflowStage]

	// Not all marking workflows are suitable for exams
	def validForExams: Boolean = false
	def permissionsParents: Stream[Department] = Option(department).toStream
}

@Entity @DiscriminatorValue("single")
class SingleMarkerWorkflow extends CM2MarkingWorkflow {
	def allStages: Seq[MarkingWorkflowStage] =  Seq(SingleMarker)
	def initialStages: Seq[MarkingWorkflowStage] = Seq(SingleMarker)
}

object SingleMarkerWorkflow {
	def apply(name: String, department: Department, firstMarkers: Seq[User]): SingleMarkerWorkflow  = {

		val singleWorkflow = new SingleMarkerWorkflow
		singleWorkflow.name = name
		singleWorkflow.department = department

		val singleMarkers = new StageMarkers()
		singleMarkers.stage = SingleMarker
		singleMarkers.workflow = singleWorkflow
		firstMarkers.foreach(singleMarkers.markers.knownType.add)

		singleWorkflow.stageMarkers = JList(singleMarkers)

		singleWorkflow
	}
}

@Entity @DiscriminatorValue("dblBlind")
class DoubleBlindWorkflow extends CM2MarkingWorkflow {
	def allStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblBlndFinalMarker)
	def initialStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerB)
}

object DoubleBlindWorkflow {
	def apply(name: String, department: Department, initialMarkers: Seq[User], finalMarkers: Seq[User]) = {
		val workflow = new DoubleBlindWorkflow
		workflow.name = name
		workflow.department = department

		val initialAMarkers = new StageMarkers
		initialAMarkers.stage = DblBlndInitialMarkerA
		initialAMarkers.workflow = workflow

		val initialBMarkers = new StageMarkers
		initialBMarkers.stage = DblBlndInitialMarkerB
		initialBMarkers.workflow = workflow

		initialMarkers.foreach(user => {
			initialAMarkers.markers.knownType.add(user)
			initialBMarkers.markers.knownType.add(user)
		})

		val lastMarkers = new StageMarkers
		lastMarkers.stage = DblBlndFinalMarker
		lastMarkers.workflow = workflow
		finalMarkers.foreach(lastMarkers.markers.knownType.add)

		workflow
	}
}
