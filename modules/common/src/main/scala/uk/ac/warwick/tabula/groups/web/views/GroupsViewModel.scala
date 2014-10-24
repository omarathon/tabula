package uk.ac.warwick.tabula.groups.web.views

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{AcademicYear, WorkflowStages, WorkflowStage}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

/**
 * A selection of view model classes for passing to the template.
 *
 * See ViewModel for some common components.
 *
 * TODO building the Menu in code means deciding things like the text and icon
 * in Scala instead of Freemarker. Is this bad? Maybe just contain key decisions
 * like whether we are a manager, then the template can decide whether to render
 * those items.
 */
object GroupsViewModel {

	case class ViewModules(
		moduleItems: Seq[ViewModule],
		canManageDepartment: Boolean
	) {
		def hasUnreleasedGroupsets : Boolean = hasUnreleasedGroupsets(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		def hasUnreleasedGroupsets(academicYear: AcademicYear) : Boolean = moduleItems.exists(_.hasUnreleasedGroupsets(academicYear))
		def hasOpenableGroupsets = moduleItems.exists(_.hasOpenableGroupsets)
		def hasCloseableGroupsets = moduleItems.exists(_.hasCloseableGroupsets)
	}

	case class ViewModule(
		module: Module,
		setItems: Seq[ViewSet],
		canManageGroups: Boolean
	) {
		def hasUnreleasedGroupsets(academicYear: AcademicYear) = module.hasUnreleasedGroupSets(academicYear)
		def hasOpenableGroupsets = module.groupSets.asScala.exists(s => (!s.openForSignups) && s.allocationMethod == SmallGroupAllocationMethod.StudentSignUp )
		def hasCloseableGroupsets = module.groupSets.asScala.exists(s => (s.openForSignups) && s.allocationMethod == SmallGroupAllocationMethod.StudentSignUp )
	}

	trait ViewSetMethods {
		def set: SmallGroupSet
		def groups: Seq[SmallGroup]
		def viewerRole: ViewerRole

		def viewerIsStudent = (viewerRole == StudentAssignedToGroup )|| (viewerRole == StudentNotAssignedToGroup)
		def viewerMustSignUp = (viewerRole == StudentNotAssignedToGroup) && isStudentSignUp && set.openForSignups
		def canViewMembers = viewerRole == Tutor || set.studentsCanSeeOtherMembers
		def canViewTutors = viewerRole == Tutor || set.studentsCanSeeTutorName
		def isStudentSignUp = set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp
		def isLinked = set.allocationMethod == SmallGroupAllocationMethod.Linked
	}

	case class ViewSet(
		set: SmallGroupSet,
		groups: Seq[SmallGroup],
		viewerRole: ViewerRole
	) extends ViewSetMethods

	case class SetProgress(
		val percentage: Int,
		val t: String,
		val messageCode: String
	)

	case class ViewSetWithProgress(
		set: SmallGroupSet,
		groups: Seq[SmallGroup],
		viewerRole: ViewerRole,
		progress: SetProgress,
		nextStage: Option[WorkflowStage],
		stages: ListMap[String, WorkflowStages.StageProgress]
	) extends ViewSetMethods

	sealed trait ViewerRole
	case object StudentAssignedToGroup extends ViewerRole
	case object StudentNotAssignedToGroup extends ViewerRole
	case object Tutor extends ViewerRole

}
