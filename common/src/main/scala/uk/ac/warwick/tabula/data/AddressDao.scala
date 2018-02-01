package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Address


trait AddressDaoComponent {
	val addressDao: AddressDao
}

trait AutowiringAddressDaoComponent extends AddressDaoComponent {
	val addressDao: AddressDao = Wire[AddressDao]
}

trait AddressDao {
	def saveOrUpdate(address: Address)
	def delete(address: Address): Unit
	def getById(id: String): Option[Address]
}

@Repository
class AddressDaoImpl extends AddressDao with Daoisms {

	def saveOrUpdate(address: Address): Unit = session.saveOrUpdate(address)

	def getById(id: String): Option[Address] = getById[Address](id)
	def delete(address: Address): Unit = session.delete(address)
}
