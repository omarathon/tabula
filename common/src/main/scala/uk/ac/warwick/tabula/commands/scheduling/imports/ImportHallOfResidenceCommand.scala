package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{Address, StudentMember}
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ImportHallOfResidenceInfoForStudentCommand {
	def apply(student: StudentMember) =
		new ImportHallOfResidenceInfoForStudentCommandInternal(student)
			with ComposableCommand[Unit]
			with ImportHallOfResidenceCommandPermissions
			with AutowiringCasUsageImporterComponent
			with AutowiringHallOfResidenceImporterComponent
			with AutowiringMemberDaoComponent
			with AutowiringAddressDaoComponent
			with Unaudited
}

class ImportHallOfResidenceInfoForStudentCommandInternal(student: StudentMember) extends CommandInternal[Unit] with PropertyCopying {

	self: HallOfResidenceImporterComponent with MemberDaoComponent with AddressDaoComponent =>

	def applyInternal(): Unit = {

		val newResidenceInfo = hallOfResidenceImporter.getResidenceInfo(student.universityId)
		newResidenceInfo match {
			case Some(rInfo) if !rInfo.isEmpty  =>
					val address = Option(student.termtimeAddress) match {
						case Some(adr: Address) => adr
						case _ => new Address()
					}
					val sourceBean = new BeanWrapperImpl(rInfo)
					val addressBean = new BeanWrapperImpl(address)
					val hasChanged = copyBasicProperties(properties, sourceBean, addressBean)
					if (hasChanged) {
						logger.debug(s"Saving term address changes for $student.universityId  with  residence address $address")
						addressDao.saveOrUpdate(address)
						student.termtimeAddress = address
						student.lastUpdatedDate = DateTime.now
						memberDao.saveOrUpdate(student)
					}
			case _ =>
				val address = student.termtimeAddress
				if (address != null) {
					student.termtimeAddress = null
					student.lastUpdatedDate = DateTime.now
					memberDao.saveOrUpdate(student)
					addressDao.delete(address)
					logger.debug(s"Removing term address for $student.universityId ")
				}
		}
	}

	private val properties = Set(
		"line1", "line2", "line3", "line4", "line5", "postcode", "telephone"
	)
}

trait ImportHallOfResidenceCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}
