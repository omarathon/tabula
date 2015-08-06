package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{MapLocation, NamedLocation, UserGroup, Module}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupServiceComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._

object EditSmallGroupSetDefaultPropertiesCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new EditSmallGroupSetDefaultPropertiesCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with EditSmallGroupSetDefaultPropertiesPermissions
			with EditSmallGroupSetDefaultPropertiesValidation
			with EditSmallGroupSetDefaultPropertiesDescription
			with AutowiringSmallGroupServiceComponent
}

trait EditSmallGroupSetDefaultPropertiesCommandState {
	def module: Module
	def set: SmallGroupSet

	var defaultWeeks: JSet[JInteger] = JSet()
	var defaultTutors: JList[String] = JArrayList()
	var defaultLocation: String = _
	var defaultLocationId: String = _
	var resetExistingEvents: Boolean = false

	def defaultWeekRanges = Option(defaultWeeks) map { weeks => WeekRange.combine(weeks.asScala.toSeq.map { _.intValue }) } getOrElse(Seq())
	def defaultWeekRanges_=(ranges: Seq[WeekRange]) {
		defaultWeeks =
			JHashSet(ranges
				.flatMap { range => range.minWeek to range.maxWeek }
				.map(i => JInteger(Some(i)))
				.toSet)
	}
}

class EditSmallGroupSetDefaultPropertiesCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[SmallGroupSet] with EditSmallGroupSetDefaultPropertiesCommandState {
	self: SmallGroupServiceComponent =>

	copyFrom(set)

	override def applyInternal() = {
		copyTo(set)

		if (resetExistingEvents) {
			set.groups.asScala.flatMap { _.events }.foreach { event =>
				event.weekRanges = set.defaultWeekRanges
				event.location = set.defaultLocation
				event.tutors.copyFrom(set.defaultTutors)
			}
		}

		smallGroupService.saveOrUpdate(set)
		set
	}

	def copyFrom(set: SmallGroupSet) {
		Option(set.defaultLocation).foreach {
			case NamedLocation(name) => defaultLocation = name
			case MapLocation(name, lid) => {
				defaultLocation = name
				defaultLocationId = lid
			}
		}

		defaultWeekRanges = set.defaultWeekRanges

		if (set.defaultTutors != null) defaultTutors.addAll(set.defaultTutors.knownType.allIncludedIds.asJava)
	}

	def copyTo(set: SmallGroupSet) {
		// If the location name has changed, but the location ID hasn't, we're changing from a map location
		// to a named location
		Option(set.defaultLocation).collect { case m: MapLocation => m }.foreach { mapLocation =>
			if (defaultLocation != mapLocation.name && defaultLocationId == mapLocation.locationId) {
				defaultLocationId = null
			}
		}

		if (defaultLocation.hasText) {
			if (defaultLocationId.hasText) {
				set.defaultLocation = MapLocation(defaultLocation, defaultLocationId)
			} else {
				set.defaultLocation = NamedLocation(defaultLocation)
			}
		} else {
			set.defaultLocation = null
		}

		set.defaultWeekRanges = defaultWeekRanges

		if (set.defaultTutors == null) set.defaultTutors = UserGroup.ofUsercodes
		set.defaultTutors.knownType.includedUserIds = defaultTutors.asScala
	}
}

trait EditSmallGroupSetDefaultPropertiesValidation extends SelfValidating {
	self: EditSmallGroupSetDefaultPropertiesCommandState =>

	override def validate(errors: Errors) {
		if (defaultLocation.safeContains("|")) errors.rejectValue("defaultLocation", "smallGroupEvent.location.invalidChar")
	}
}

trait EditSmallGroupSetDefaultPropertiesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupSetDefaultPropertiesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupSetDefaultPropertiesDescription extends Describable[SmallGroupSet] {
	self: EditSmallGroupSetDefaultPropertiesCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}