package uk.ac.warwick.tabula.commands.imports

import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import collection.JavaConversions._
import uk.ac.warwick.tabula.SprCode
import uk.ac.warwick.spring.Wire

class ImportProfilesCommand extends Command[Unit] with Logging with Daoisms {

	var profileImporter = Wire.auto[ProfileImporter]
	var profileService = Wire.auto[ProfileService]

	def applyInternal() {
		benchmark("ImportMembers") {
			doMemberDetails
			logger.debug("Imported UpstreamMembers")
		}
	}

	/** Import basic info about all members in ADS, batched 1000 at a time */
	def doMemberDetails {
		transactional() {
			benchmark("Import all member details") {
				var list = List[UpstreamMember]()
				profileImporter.allMemberDetails { member =>
					list = list :+ member
					if (list.size >= 1000) {
						logger.info("Saving details of " + list.size + " members")
						saveMemberDetails(list)
						list = Nil
					}
				}
				if (!list.isEmpty) {
					logger.info("Saving details of " + list.size + " members")
					saveMemberDetails(list)
				}
			}
		}
	}

	def saveMemberDetails(seq: Seq[UpstreamMember]) {
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