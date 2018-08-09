package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{Address, StudentMember}
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.AddressImporter.AddressInfo
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ImportHallOfResidenceInfoForStudentCommand {
	def apply(student: StudentMember) =
		new ImportHallOfResidenceInfoForStudentCommandInternal(student)
			with ComposableCommand[Unit]
			with ImportHallOfResidenceCommandPermissions
			with AutowiringCasUsageImporterComponent
			with AutowiringAddressImporterComponent
			with AutowiringMemberDaoComponent
			with AutowiringAddressDaoComponent
			with Unaudited
}

class ImportHallOfResidenceInfoForStudentCommandInternal(student: StudentMember) extends CommandInternal[Unit] with PropertyCopying {

	self: AddressImporterComponent with MemberDaoComponent with AddressDaoComponent =>

	def applyInternal(): Unit = {

		val newResidenceInfo = addressImporter.getAddressInfo(student.universityId)

		newResidenceInfo.currentAddress match {
			case Some(rInfo) if !rInfo.isEmpty  => updateAddress("currentAddress", rInfo, student.currentAddress, a => {student.currentAddress = a})
			case _ => deleteAddress("currentAddress", student.currentAddress, () => {student.currentAddress = null})
		}

		newResidenceInfo.hallOfResidence match {
			case Some(rInfo) if !rInfo.isEmpty  => updateAddress("termtimeAddress", rInfo, student.termtimeAddress, a => {student.termtimeAddress = a})
			case _ => deleteAddress("termtimeAddress", student.termtimeAddress, () => {student.termtimeAddress = null})
		}

		def updateAddress(fieldName:String, info:AddressInfo, addressCurrentValue:Address, update: Address => Unit): Unit = {
			val address = Option(addressCurrentValue).getOrElse(new Address())
			val sourceBean = new BeanWrapperImpl(info)
			val addressBean = new BeanWrapperImpl(address)
			val hasChanged = copyBasicProperties(properties, sourceBean, addressBean)
			if (hasChanged) {
				logger.debug(s"Saving $fieldName changes for $student.universityId  with  residence address $address")
				addressDao.saveOrUpdate(address)
				update(address)
				student.lastUpdatedDate = DateTime.now
				memberDao.saveOrUpdate(student)
			}
		}

		def deleteAddress(fieldName:String, address:Address, delete: () => Unit): Unit = {
			if (address != null) {
				delete()
				student.lastUpdatedDate = DateTime.now
				memberDao.saveOrUpdate(student)
				addressDao.delete(address)
				logger.debug(s"Removing $fieldName for $student.universityId ")
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
