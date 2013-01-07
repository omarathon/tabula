package uk.ac.warwick.tabula.commands.imports

import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.SprCode
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.Features

class ImportProfilesCommand extends Command[Unit] with Logging with Daoisms {

	var profileImporter = Wire.auto[ProfileImporter]
	var profileService = Wire.auto[ProfileService]
	var userLookup = Wire.auto[UserLookupService]
	var features = Wire.auto[Features]

	def applyInternal() {
		if (features.profiles) {
			benchmark("ImportMembers") {
				doMemberDetails
				logger.debug("Imported Members")
				doAddressDetails
				doNextOfKinDetails
			}
		}
	}

	/** Import basic info about all members in ADS, batched 250 at a time (small batch size is mostly for web sign-on's benefit) */
	def doMemberDetails {
		benchmark("Import all member details") {
			for (usercodes <- logSize(profileImporter.allUserCodes).grouped(250)) {
				logger.info("Fetching details for " + usercodes.size + " usercodes from websignon")
				val users: Map[String, User] = userLookup.getUsersByUserIds(usercodes).toMap
				
				logger.info("Fetching member details for " + usercodes.size + " members from ADS")

				transactional() {
					profileImporter.getMemberDetails(usercodes).map(profileImporter.processNames(_, users)) map { _.apply }
					session.flush
					session.clear
				}
			}
		}
	}
	
	def doAddressDetails {
		
	}
	
	def doAddressDetails(member: Member) {
		if (member.homeAddress != null) session.delete(member.homeAddress)
		if (member.termtimeAddress != null) session.delete(member.termtimeAddress)
	  
		for (address <- profileImporter.getAddresses(member)) {
			address.addressType match {
			  	case Home => member.homeAddress = address
			  	case TermTime => member.termtimeAddress = address
			}
		}
	}
	
	def doNextOfKinDetails {
		
	}
	
	def doNextOfKinDetails(member: Member) {
		for (kin <- member.nextOfKins) session.delete(kin)
		
		val kins = profileImporter.getNextOfKins(member)
		member.nextOfKins.addAll(kins)
		for (kin <- kins) kin.member = member
	}
	
	def refresh(member: Member) {	  
//		transactional() {
//			// Delete old cruft
//			if (member.homeAddress != null) session.delete(member.homeAddress)
//			if (member.termtimeAddress != null) session.delete(member.termtimeAddress)		
//			for (kin <- member.nextOfKins) session.delete(kin)
//		}
//				
		transactional() {		
			val usercode = member.userId
			val user = userLookup.getUserByUserId(usercode)
		
			val memberCommands = profileImporter.getMemberDetails(List(usercode)).map(profileImporter.processNames(_, Map(usercode -> user)))
			val members = memberCommands map { _.apply }
			
			session.flush
			for (member <- members) session.evict(member)
			
//			val newMember = members.head
//			
//			doAddressDetails(newMember)
//			doNextOfKinDetails(newMember)
//			
//			saveMemberDetails(Seq(newMember))
		}
	}
	
	def equal(s1: Seq[String], s2: Seq[String]) =
		s1.length == s2.length && s1.sorted == s2.sorted

	def describe(d: Description) {

	}

}