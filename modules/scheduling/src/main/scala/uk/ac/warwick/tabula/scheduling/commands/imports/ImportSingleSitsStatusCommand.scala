package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet


import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying

class ImportSingleSitsStatusCommand(resultSet: ResultSet) extends Command[SitsStatus] with Logging with Daoisms 
	with Unaudited with PropertyCopying {
	
	PermissionCheck(Permissions.ImportSystemData)
	
	var sitsStatusDao = Wire.auto[SitsStatusDao]

	// A couple of intermediate properties that will be transformed later
	var code: String = _
	var shortName: String = _
	var fullName: String = _
	
	this.code = resultSet.getString("sta_code")
	this.shortName = resultSet.getString("sta_snam")
	this.fullName = resultSet.getString("sta_name")
	
	override def applyInternal(): SitsStatus = transactional() {
		val sitsStatusExisting = sitsStatusDao.getByCode(code)
		
		logger.debug("Importing SITS status " + code + " into " + sitsStatusExisting)
		
		val isTransient = !sitsStatusExisting.isDefined
		
		val sitsStatus = sitsStatusExisting match {
			case Some(sitsStatus: SitsStatus) => sitsStatus
			case _ => new SitsStatus()
		}
		
		val commandBean = new BeanWrapperImpl(this)
		val sitsStatusBean = new BeanWrapperImpl(sitsStatus)
		
		val hasChanged = copyBasicProperties(properties, commandBean, sitsStatusBean)
			
		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + sitsStatus)
			
			sitsStatus.lastUpdatedDate = DateTime.now
			sitsStatusDao.saveOrUpdate(sitsStatus)
		}
		
		sitsStatus
	}
	
	private val properties = Set(
		"code", "shortName", "fullName"
	)
	
	override def describe(d: Description) = d.property("shortName" -> shortName)

}
