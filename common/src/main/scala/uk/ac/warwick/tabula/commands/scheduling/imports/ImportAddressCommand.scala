package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{Address, Member, StudentMember}
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.AddressImporter.AddressInfo
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ImportAddressCommand {
	def apply(member: Member) =
		new ImportAddressCommandInternal(member)
			with ComposableCommand[Unit]
			with ImportAddressCommandPermissions
			with AutowiringCasUsageImporterComponent
			with AutowiringAddressImporterComponent
			with AutowiringMemberDaoComponent
			with AutowiringAddressDaoComponent
			with Unaudited
}

class ImportAddressCommandInternal(member: Member) extends CommandInternal[Unit] with PropertyCopying {

	self: AddressImporterComponent with MemberDaoComponent with AddressDaoComponent =>

	def applyInternal(): Unit = {

		val newAddressInfo = addressImporter.getAddressInfo(member.universityId)

		newAddressInfo.currentAddress match {
			case Some(rInfo) if !rInfo.isEmpty  => updateAddress("currentAddress", rInfo, member.currentAddress, a => {member.currentAddress = a})
			case _ => deleteAddress("currentAddress", member.currentAddress, () => {member.currentAddress = null})
		}

		Option(member).collect { case student: StudentMember =>
			newAddressInfo.hallOfResidence match {
				case Some(rInfo) if !rInfo.isEmpty  => updateAddress("termtimeAddress", rInfo, student.termtimeAddress, a => {student.termtimeAddress = a})
				case _ => deleteAddress("termtimeAddress", student.termtimeAddress, () => {student.termtimeAddress = null})
			}
		}

		def updateAddress(fieldName:String, info:AddressInfo, addressCurrentValue:Address, update: Address => Unit): Unit = {
			val address = Option(addressCurrentValue).getOrElse(new Address())
			val sourceBean = new BeanWrapperImpl(info)
			val addressBean = new BeanWrapperImpl(address)
			val hasChanged = copyBasicProperties(properties, sourceBean, addressBean)
			if (hasChanged) {
				logger.debug(s"Saving $fieldName changes for $member.universityId  with  residence address $address")
				addressDao.saveOrUpdate(address)
				update(address)
				member.lastUpdatedDate = DateTime.now
				memberDao.saveOrUpdate(member)
			}
		}

		def deleteAddress(fieldName:String, address:Address, delete: () => Unit): Unit = {
			if (address != null) {
				delete()
				member.lastUpdatedDate = DateTime.now
				memberDao.saveOrUpdate(member)
				addressDao.delete(address)
				logger.debug(s"Removing $fieldName for $member.universityId ")
			}
		}

	}

	private val properties = Set(
		"line1", "line2", "line3", "line4", "line5", "postcode", "telephone"
	)
}

trait ImportAddressCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}
