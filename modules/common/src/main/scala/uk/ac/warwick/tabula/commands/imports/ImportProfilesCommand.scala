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

class ImportProfilesCommand extends Command[Unit] with Logging with Daoisms {

	var profileImporter = Wire.auto[ProfileImporter]
	var profileService = Wire.auto[ProfileService]
	var userLookup = Wire.auto[UserLookupService]

	def applyInternal() {
		benchmark("ImportMembers") {
			doMemberDetails
			logger.debug("Imported Members")
			doAddressDetails
			doNextOfKinDetails
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
					saveMemberDetails(profileImporter.getMemberDetails(usercodes).map(profileImporter.processNames(_, users)))
				}
			}
		}
	}
	
	def doAddressDetails {
		
	}
	
	def doAddressDetails(member: Member) {
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
		member.nextOfKins.clear
		member.nextOfKins.addAll(profileImporter.getNextOfKins(member))
	}
	
	def refresh(member: Member) {
		// The importer creates a new object and does saveOrUpdate; so evict the current object
		session.evict(member)
		val usercode = member.userId
		val user = userLookup.getUserByUserId(usercode)
	  
		transactional() {
			val members = profileImporter.getMemberDetails(List(usercode)).map(profileImporter.processNames(_, Map(usercode -> user)))
			saveMemberDetails(members)
			
			val newMember = members.head
			doAddressDetails(newMember)
			doNextOfKinDetails(newMember)
			
			session.update(newMember)
			session.flush
		}
	}
	
	def saveMemberDetails(seq: Seq[Member]) {
		seq foreach { member =>
			session.saveOrUpdate(member)
		}
		session.flush
		seq foreach session.evict
	}
	
	def equal(s1: Seq[String], s2: Seq[String]) =
		s1.length == s2.length && s1.sorted == s2.sorted

	def describe(d: Description) {

	}

}