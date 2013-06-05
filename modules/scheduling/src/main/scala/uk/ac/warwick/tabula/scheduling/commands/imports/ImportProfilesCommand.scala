package uk.ac.warwick.tabula.scheduling.commands.imports
import scala.collection.JavaConversions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.scheduling.services.ProfileImporter
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation

class ImportProfilesCommand extends Command[Unit] with Logging with Daoisms {

	PermissionCheck(Permissions.ImportSystemData)

	var madService = Wire.auto[ModuleAndDepartmentService]
	var profileImporter = Wire.auto[ProfileImporter]
	var profileService = Wire.auto[ProfileService]
	var userLookup = Wire.auto[UserLookupService]
	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	var modeOfAttendanceImporter = Wire.auto[ModeOfAttendanceImporter]

	var features = Wire.auto[Features]

	val BatchSize = 250

	def applyInternal() {
		if (features.profiles) {
			benchmark("ImportMembers") {
				importSitsStatuses
				importModeOfAttendances
				doMemberDetails
			}
		}
	}

	def importSitsStatuses {
		logger.info("Importing SITS statuses")

		transactional() {
			sitsStatusesImporter.getSitsStatuses() map { _.apply }

			session.flush
			session.clear
		}
	}

	def importModeOfAttendances {
		logger.info("Importing Modes of Attendance")

		transactional() {
			modeOfAttendanceImporter.getModeOfAttendances() map { _.apply }

			session.flush
			session.clear
		}
	}

	/** Import basic info about all members in ADS, batched 250 at a time (small batch size is mostly for web sign-on's benefit) */
	def doMemberDetails {
		benchmark("Import all member details") {
			for {
				department <- madService.allDepartments;
				userIdsAndCategories <- logSize(profileImporter.userIdsAndCategories(department)).grouped(BatchSize)
			} {
				logger.info("Fetching user details for " + userIdsAndCategories.size + " usercodes from websignon")
				val users: Map[String, User] = userLookup.getUsersByUserIds(userIdsAndCategories.map(x => x.member.usercode)).toMap

				logger.info("Fetching member details for " + userIdsAndCategories.size + " members from ADS")

				transactional() {
					profileImporter.getMemberDetails(userIdsAndCategories, users) map { _.apply }

					session.flush
					session.clear
				}
			}
		}
	}

	def refresh(member: Member) {
		transactional() {
			val usercode = member.userId
			val user = userLookup.getUserByUserId(usercode)

			profileImporter.userIdAndCategory(member) match {
				case membInfo: MembershipInformation => {
					val members = profileImporter.getMemberDetails(List(membInfo), Map(usercode -> user)) map { _.apply }
					session.flush
					for (member <- members) session.evict(member)
				}
				case _ => logger.warn("Student is no longer in uow_current_members in membership - not updating")
			}
		}
	}

	def equal(s1: Seq[String], s2: Seq[String]) =
		s1.length == s2.length && s1.sorted == s2.sorted

	def describe(d: Description) {

	}

}