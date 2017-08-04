
package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.timetables.{AutowiringWAI2GoConfigurationComponent, LocationFetchingServiceComponent, WAI2GoHttpLocationFetchingServiceComponent}

object ImportSmallGroupEventLocationsCommand {
	def apply(academicYear: AcademicYear) = new ComposableCommand[Unit]
		with ImportSmallGroupEventLocationsCommand
		with ImportSmallGroupEventLocationsDescription
		with AutowiringWAI2GoConfigurationComponent
		with WAI2GoHttpLocationFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent {
			val year: AcademicYear = academicYear
		}
}


trait ImportSmallGroupEventLocationsCommand extends CommandInternal[Unit] with RequiresPermissionsChecking with Logging {

	self: LocationFetchingServiceComponent with SmallGroupServiceComponent =>

	val year: AcademicYear

	def permissionsCheck(p:PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

	def applyInternal() {
		logger.info(s"Finding events without a location in ${year.toString} ...")
		benchmark("ImportEventLocations") {
			val events: Seq[SmallGroupEvent] = transactional(readOnly = true) { smallGroupService.listSmallGroupsWithoutLocation(year) }
			logger.info(s"Found ${events.size} events without a location in ${year.toString} ...")
			events.map(event => {
				val newLocation = locationFetchingService.locationFor(event.location.name)
				event.location = newLocation
				smallGroupService.saveOrUpdate(event)
				event
			})
		}
	}

}


trait ImportSmallGroupEventLocationsDescription extends Describable[Unit] {
	override val eventName = "ImportSmallGroupEventLocations"
	def describe(d: Description) {}
}