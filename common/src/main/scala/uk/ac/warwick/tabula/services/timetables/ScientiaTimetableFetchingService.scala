package uk.ac.warwick.tabula.services.timetables

import dispatch.classic.{Handler, Request, url}
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.{DateTimeConstants, LocalTime}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.MapLocation
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRangeListUserType}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{ClockComponent, FoundUser, Futures, Logging}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

import scala.concurrent.Future
import scala.xml.Elem

trait ScientiaConfiguration {
	val perYearUris: Seq[(String, AcademicYear)]
	val cacheSuffix: String
	val cacheExpiryTime: Int
	val returnEvents: Boolean = true
}

trait ScientiaConfigurationComponent {
	val scientiaConfiguration: ScientiaConfiguration
}

trait AutowiringScientiaConfigurationComponent extends ScientiaConfigurationComponent with ClockComponent {
	val scientiaConfiguration = new AutowiringScientiaConfiguration

	class AutowiringScientiaConfiguration extends ScientiaConfiguration {
		def scientiaFormat(year: AcademicYear): String = {
			// e.g. 1314
			(year.startYear % 100).toString + (year.endYear % 100).toString
		}

		lazy val scientiaBaseUrl: String = Wire.optionProperty("${scientia.base.url}").getOrElse("https://test-timetablingmanagement.warwick.ac.uk/xml")
		lazy val currentAcademicYear: Option[AcademicYear] = Some(AcademicYear.guessSITSAcademicYearByDate(clock.now))
		lazy val prevAcademicYear: Option[AcademicYear] = {
			// TAB-3074 we only fetch the previous academic year if the month is >= AUGUST and < OCTOBER
			val month = clock.now.getMonthOfYear
			if (month >= DateTimeConstants.AUGUST && month < DateTimeConstants.OCTOBER)
				currentAcademicYear.map(_ - 1)
			else
				None
		}

		def yearProperty: Option[Seq[AcademicYear]] =
			Wire.optionProperty("${scientia.years}").map { _.split(",").map(AcademicYear.parse) }

		lazy val academicYears: Seq[AcademicYear] = yearProperty.getOrElse { Seq(prevAcademicYear, currentAcademicYear).flatten }

		lazy val perYearUris: Seq[(String, AcademicYear)] = academicYears.map { year => (scientiaBaseUrl + scientiaFormat(year) + "/", year) }

		lazy val cacheSuffix: String = Wire.optionProperty("${scientia.cacheSuffix}").getOrElse("")

		override val cacheExpiryTime: Int = 60 * 60 // 1 hour in seconds

	}

}

trait ScientiaHttpTimetableFetchingServiceComponent extends CompleteTimetableFetchingServiceComponent {
	self: ScientiaConfigurationComponent =>

	lazy val timetableFetchingService = new CombinedTimetableFetchingService(
		ScientiaHttpTimetableFetchingService(scientiaConfiguration)
	)
}

private class ScientiaHttpTimetableFetchingService(scientiaConfiguration: ScientiaConfiguration) extends CompleteTimetableFetchingService with Logging {
	self: LocationFetchingServiceComponent
		with SmallGroupServiceComponent
		with ModuleAndDepartmentServiceComponent
		with UserLookupComponent
		with DispatchHttpClientComponent =>

	import ScientiaHttpTimetableFetchingService._

	lazy val perYearUris: Seq[(String, AcademicYear)] = scientiaConfiguration.perYearUris

	lazy val studentUris: Seq[(String, AcademicYear)] = perYearUris.map {
		case (uri, year) => (uri + "?StudentXML", year)
	}
	lazy val staffUris: Seq[(String, AcademicYear)] = perYearUris.map {
		case (uri, year) => (uri + "?StaffXML", year)
	}
	lazy val courseUris: Seq[(String, AcademicYear)] = perYearUris.map {
		case (uri, year) => (uri + "?CourseXML", year)
	}
	lazy val moduleNoStudentsUris: Seq[(String, AcademicYear)] = perYearUris.map {
		case (uri, year) => (uri + "?ModuleNoStudentsXML", year)
	}
	lazy val moduleWithSudentsUris: Seq[(String, AcademicYear)] = perYearUris.map {
		case (uri, year) => (uri + "?ModuleXML", year)
	}
	lazy val roomUris: Seq[(String, AcademicYear)] = perYearUris.map {
		case (uri, year) => (uri + "?RoomXML", year)
	}

	// a dispatch response handler which reads XML from the response and parses it into a list of TimetableEvents
	// the timetable response doesn't include its year, so we pass that in separately.
	def handler(year: AcademicYear, excludeSmallGroupEventsInTabula: Boolean = false, uniId: String): (Map[String, Seq[String]], Request) => Handler[Seq[TimetableEvent]] = { (_: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req <> { node =>
			parseXml(node, year, uniId, locationFetchingService, moduleAndDepartmentService, userLookup)
		}
	}

	private def hasSmallGroups(moduleCode: Option[String], year: AcademicYear) =
		moduleCode.flatMap(moduleAndDepartmentService.getModuleByCode).fold(false) { module =>
			!smallGroupService.getSmallGroupSets(module, year).forall(_.archived)
		}

	def getTimetableForStudent(universityId: String): Future[EventList] = doRequest(studentUris, universityId, excludeSmallGroupEventsInTabula = true)
	def getTimetableForModule(moduleCode: String, includeStudents: Boolean): Future[EventList] = {
		if (includeStudents) doRequest(moduleWithSudentsUris, moduleCode)
		else doRequest(moduleNoStudentsUris, moduleCode)
	}
	def getTimetableForCourse(courseCode: String): Future[EventList] = doRequest(courseUris, courseCode)
	def getTimetableForRoom(roomName: String): Future[EventList] = doRequest(roomUris, roomName)
	def getTimetableForStaff(universityId: String): Future[EventList] = doRequest(
		staffUris,
		universityId,
		excludeSmallGroupEventsInTabula = true,
		excludeEventTypes = Seq(TimetableEventType.Seminar, TimetableEventType.Practical)
	)

	def doRequest(
		uris: Seq[(String, AcademicYear)],
		param: String,
		excludeSmallGroupEventsInTabula: Boolean = false,
		excludeEventTypes: Seq[TimetableEventType] = Seq()
	): Future[EventList] = {
		// fetch the events from each of the supplied URIs, and flatmap them to make one big list of events
		val results: Seq[Future[EventList]] = uris.map { case (uri, year) =>
			// add ?p0={param} to the URL's get parameters
			val req = url(uri) <<? Map("p0" -> param)
			// execute the request.
			// If the status is OK, pass the response to the handler function for turning into TimetableEvents
			// else return an empty list.
			logger.info(s"Requesting timetable data from ${req.to_uri.toString}")

			val result = Future {
				val ev = httpClient.when(_==200)(req >:+ handler(year, excludeSmallGroupEventsInTabula, param))

				if (ev.isEmpty) {
					logger.info(s"Timetable request successful but no events returned: ${req.to_uri.toString}")
				}

				ev
			}

			// Some extra logging here
			result.onFailure { case e =>
				logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
			}

			result.map { events =>
				if (excludeSmallGroupEventsInTabula)
					EventList.fresh(events.filterNot { event =>
						event.eventType == TimetableEventType.Seminar &&
							hasSmallGroups(event.parent.shortName, year)
					})
				else EventList.fresh(events)
			}.map(events => events.filterNot(e => excludeEventTypes.contains(e.eventType)))
		}

		Futures.combine(results, EventList.combine).map(eventsList =>
			if (!scientiaConfiguration.returnEvents) {
				EventList.empty
			} else if (eventsList.events.isEmpty) {
				logger.info(s"All timetable years are empty for $param")
				throw new TimetableEmptyException(uris, param)
			} else {
				eventsList
			}
		)
	}

}

class TimetableEmptyException(val uris: Seq[(String, AcademicYear)], val param: String)
	extends IllegalStateException(s"Received empty timetables for $param using: ${uris.map { case (uri, _) => uri}.mkString(", ") }")

object ScientiaHttpTimetableFetchingService extends Logging {

	val cacheName = "SyllabusPlusTimetableLists"

	def apply(scientiaConfiguration: ScientiaConfiguration): CompleteTimetableFetchingService = {
		val service =
			new ScientiaHttpTimetableFetchingService(scientiaConfiguration)
				with WAI2GoHttpLocationFetchingServiceComponent
				with AutowiringSmallGroupServiceComponent
				with AutowiringModuleAndDepartmentServiceComponent
				with AutowiringWAI2GoConfigurationComponent
				with AutowiringUserLookupComponent
				with AutowiringDispatchHttpClientComponent

		if (scientiaConfiguration.perYearUris.exists(_._1.contains("stubTimetable"))) {
			// don't cache if we're using the test stub - otherwise we won't see updates that the test setup makes
			service
		} else {
			new CachedCompleteTimetableFetchingService(service, s"$cacheName${scientiaConfiguration.cacheSuffix}", scientiaConfiguration.cacheExpiryTime)
		}
	}

	// Big ugly hardcoded map of all the rooms in the timetabling system
	private final val CentrallyManagedRooms: Map[String, String] = Map(
		"IB_2.011" -> "25555", "FAC.ART_H4.54" -> "27050", "HI_H3.47" -> "27039", "FAC.ART_H4.55" -> "27051", "ET_S1.88" -> "37954", "ES_F5.06" -> "31379",
		"ES_IMC" -> "23988", "ES_RADCLIFFE" -> "23895", "H5.45" -> "21777", "ES_D2.12" -> "51315", "ES_D2.27" -> "51329", "BS_E110" -> "35374",
		"HA_F25B" -> "28567", "IB_3.003" -> "51589", "IB_2.006" -> "51541", "IB_2.005" -> "51542", "IB_0.011" -> "33925", "IB_0.009" -> "33926",
		"IB_0.103" -> "33937", "IB_1.015" -> "25471", "IB_0.102" -> "33977", "IB_2.004" -> "51528", "IB_2.003" -> "51521", "IB_3.006" -> "25692",
		"IB_1.009" -> "25464", "IB_0.006" -> "33929", "MA_B1.01" -> "41202", "FAC.SS_S2.77" -> "37695", "ET_A1.11" -> "38185", "ES_F1.05" -> "30927",
		"HI_H3.45" -> "21598", "WM_0.04" -> "19673", "MD_MSBB0.26" -> "36999", "PO_B0.06" -> "37576", "PS_H1.48a" -> "26967", "IL_H0.76" -> "21375",
		"WM_1.06" -> "30928", "TH_G.51" -> "28504", "CH_LABS4" -> "44912", "CH_LABS3" -> "44802", "FI_A1.27" -> "28586", "F.25B (Millburn)" -> "28567",
		"A0.26 (Millburn)" -> "28459", "A1.25 (Millburn)" -> "28587", "PX_P5.23" -> "35460", "IL_R0.12" -> "45706", "ST_C1.06" -> "41240", "PX_P3.45" -> "35574",
		"IB_M2" -> "29373", "A0.28 (Millburn)" -> "28461", "CH_C5.23" -> "44977", "H5.22" -> "21718", "EN_H5.07" -> "21761", "EN_H5.43" -> "21725",
		"MD_MTC1.04" -> "37247", "MD_MTC1.05" -> "37251", "MD_MTC1.06" -> "37246", "MD0.01" -> "20754", "FI_A0.26" -> "28459", "A1.28 (Millburn)" -> "28585",
		"MS.01" -> "40858", "MS.05" -> "29048", "MS.02" -> "40879", "MS.04" -> "29046", "MS.03" -> "29050", "LA_S1.14" -> "38035", "ARTS-CINEMA" -> "41920",
		"P5.21" -> "35461", "IMC0.02" -> "19679", "IN_A0.01 (PC room - Linux)" -> "40793", "MAS_CONCOURSE" -> "30943", "H3.05" -> "21584", "MAS_2.06" -> "25512",
		"MAS_2.03" -> "21518", "MAS_2.04" -> "21519", "MAS_2.05" -> "33352", "H0.01" -> "21318", "ES_A2.06" -> "31034", "ES_F0.24" -> "30680",
		"ES_F1.04" -> "30926", "EC_S2.79" -> "37696", "IN_A0.03 (PC room - Zeeman)" -> "40810", "GLT3" -> "36854", "WT0.04" -> "38983", "WT0.02" -> "38986",
		"WT0.03" -> "38987", "EP_WT0.01" -> "38973", "WT0.05" -> "38982", "EP_WT0.06 (lc)" -> "38964", "WT1.01" -> "38997", "EP_WT1.03 (lab)" -> "38995",
		"WT1.04" -> "39005", "WT1.04/5" -> "39005", "WT1.05" -> "39004", "EP_WT1.06 (lab)" -> "38993", "ES_D0.02" -> "30656", "ES_F0.03" -> "30673",
		"ES_A1.16" -> "30969", "ES_F2.10" -> "31017", "ES_A2.02" -> "31012", "ES_A0.08" -> "30689", "MD_MSBA0.41" -> "36945", "MD_MSBA0.30" -> "36946",
		"MD_MSBA0.42" -> "36904", "MD_MSBA1.50" -> "37173", "H3.58" -> "27042", "PO_S1.50" -> "37989", "PX_PS0.18" -> "27428", "H2.46" -> "21532",
		"H3.02" -> "21583", "E0.23 (Soc Sci)" -> "37501", "H4.03" -> "21658", "ES_F1.06" -> "30928", "IL_REINV" -> "47252", "ES_F3.08" -> "31197",
		"MA_B3.02" -> "29096", "H1.03" -> "21430", "HI_H3.03" -> "21584", "H0.05" -> "21315", "H2.44" -> "21536", "H0.02" -> "21317", "EN_H5.42" -> "21721",
		"S0.17" -> "37481", "WOODS-SCAWEN" -> "42011", "L4" -> "31390", "S0.10" -> "37490", "S0.13" -> "37476", "S0.18" -> "37482", "S1.141" -> "37910",
		"S0.19" -> "37483", "GLT1" -> "37286", "H0.51" -> "21336", "H3.44" -> "21601", "WLT" -> "38986", "B2.04/5 (Sci Conc)" -> "31395",
		"A0.05 (Soc Sci)" -> "37626", "L3" -> "31456", "S0.28" -> "37406", "S0.20" -> "37484", "PLT" -> "35601", "S0.21" -> "37486", "F1.07" -> "30933",
		"S2.73" -> "37727", "F1.10" -> "30918", "H0.52" -> "21336", "H4.45" -> "21670", "H0.03" -> "21316", "GLT2" -> "37284", "S0.09" -> "37491",
		"H0.58" -> "21346", "S1.66" -> "37944", "S1.69" -> "37916", "H1.48" -> "26971", "B2.03 (Sci Conc)" -> "31396", "B2.01 (Sci Conc)" -> "31399",
		"WE0.01" -> "47650", "LIB2" -> "38872", "S0.11" -> "37478", "H0.60" -> "21347", "H2.45" -> "21538", "S2.84" -> "37731", "S0.52" -> "37419",
		"F1.11" -> "30917", "H2.03" -> "21518", "EQ_C1.11/15" -> "38082", "ARTS-BUTTERWORTH" -> "42086", "B2.02 (Sci Conc)" -> "31403", "WCE0.12" -> "45808",
		"W.MUSIC1" -> "20441", "WCE0.9b" -> "45814", "H1.07" -> "21426", "L5" -> "31389", "LIB1" -> "38890", "H4.02" -> "21659", "S0.08" -> "37492",
		"W.MUSIC2" -> "20444", "W.MUSIC3" -> "20447", "CX_H2.04" -> "21519", "IT_H4.03" -> "21658", "FR_H4.43" -> "21643", "FR_H4.44" -> "21642",
		"CS_CS1.04" -> "26858", "GE_H2.02" -> "21517", "IB_0.013" -> "33994", "WA0.15" -> "19139", "WA1.01" -> "19194", "PS1.28" -> "27644", "WA1.09" -> "19189",
		"WA1.15" -> "19198", "WCE0.9a" -> "45815", "LB_TEACHING" -> "50520", "R0.21" -> "45730", "R0.03/4" -> "45735", "R0.14" -> "45705", "R1.15" -> "45291",
		"R2.41" -> "27745", "R3.41" -> "27786", "R0.12" -> "45706", "R1.13" -> "41355", "R1.03" -> "45294", "R1.04" -> "45278", "S2.81" -> "37697",
		"H3.55" -> "27029", "R3.25" -> "27762", "LL_H0.64" -> "21354", "LL_H0.61" -> "21345", "IP_R3.38" -> "27783", "LL_H0.66" -> "21356", "WCE0.10" -> "45813",
		"CS_CS1.01" -> "26849", "CS_CS0.03" -> "26800", "EN_H5.01" -> "21760", "H4.01" -> "21700", "IN_R0.41 (PC room - Library)" -> "38883",
		"IN_R0.39 (PC room - Library)" -> "38886", "IN_S2.74 (PC room - Soc Sci)" -> "37688", "EC_S2.86" -> "37733", "LF_ICLS" -> "35380", "PS_H1.49a" -> "52919",
		"MS.B3.03" -> "29094", "S0.50" -> "37421", "PX_CONCOURSE" -> "51236", "FI_A1.25" -> "28587", "ST_C0.01" -> "40876", "WM_1.04" -> "26858",
		"BS_BSR1" -> "35398", "BS_BSR4" -> "36984", "BS_BSR2" -> "35399", "BS_BSR5" -> "36985", "BS_LOCTUTS" -> "36985", "ST_A1.01" -> "19194", "IB_M1" -> "37315",
		"IN_A0.02 (PC room - Zeeman)" -> "40811", "PX_P5.64" -> "35446", "H4.22/4" -> "21669", "H0.43" -> "21402", "H0.56" -> "21339", "MA_B3.01" -> "29111",
		"OC0.03" -> "52117", "WA1.10" -> "19195", "WA1.20" -> "19213", "IN_MM1 (PC room - Westwood)" -> "50593", "OC0.02" -> "52139", "A0.23 (Soc Sci)" -> "37638",
		"H3.56" -> "27030", "H3.57" -> "27031", "H0.44" -> "21327", "CS_CS0.01" -> "26811", "LA_S0.03" -> "37496", "LA_S0.04" -> "50947", "TH_G.56" -> "28405",
		"TH_G.52" -> "28425", "TH_G.50" -> "28426", "TH_G.53" -> "28406", "F.25A (Millburn)" -> "28613", "HA_F37" -> "28566", "SM_336" -> "21621",
		"EN_G.03" -> "28377", "TH_G.31" -> "28490", "OC0.04" -> "52144", "LA_S2.12" -> "33364", "IN_B0.52 (PC room - G.Hill)" -> "37017", "BS_E018" -> "35310",
		"BS_E109" -> "35375", "ES_F2.11" -> "30952", "ES_F2.15" -> "31019", "ES_D0.09" -> "30650", "H1.02" -> "21431", "CH_C5.06" -> "44964", "CH_C5.21" -> "44976",
		"ET_A0.14" -> "37641", "ET_A1.05" -> "38177", "ET_S1.71" -> "37917", "ET_S2.85" -> "37732", "ES_D2.02" -> "51334", "ES_A4.01" -> "31329",
		"MD_MTC0.04" -> "36906", "MD_MTC0.05" -> "37041", "MD_MTC0.06" -> "20712", "MD_MTC0.07" -> "36909", "MD_MTC0.08" -> "36911", "MD_MTC0.09" -> "20715",
		"MD_MTC0.10" -> "36914", "MD_MTC0.11" -> "37042", "MD_MTC1.08" -> "37245", "MD_MTC1.10" -> "37244", "MD_MTC1.11" -> "37254", "MD_MTC1.09" -> "37253",
		"GLT4" -> "37016", "CE_Pigeon Loft" -> "20240", "IB_0.301" -> "33920", "IB_1.301" -> "25466", "CS_CS0.07" -> "26816", "TH_G.55" -> "28410",
		"IL_G.57" -> "28491", "EN_G.08" -> "28378", "ES_F0.25a" -> "30679", "ES_F0.25" -> "30679", "PX_PS0.17a" -> "27427", "LL_H0.78" -> "21359",
		"IB_3.007" -> "25685", "LL_H0.67" -> "21411", "IR_B0.41/43" -> "37605", "ST_C0.08" -> "37663", "IB_1.003" -> "51379", "IB_1.002b" -> "51378",
		"IB_1.002a" -> "51377", "IB_1.002" -> "51376", "IB_0.004" -> "51478", "IB_0.002a" -> "51437", "IB_0.002" -> "51433", "IB_1.007" -> "51423",
		"IB_1.006" -> "51400", "IB_1.005" -> "51401", "IB_2.007" -> "51540", "IB_SH8" -> "29303", "IB_SH7" -> "29307", "IB_SH6" -> "29305", "IB_SH5" -> "29391",
		"IB_SH4" -> "29306", "IB_SH3" -> "29392", "IB_SH2" -> "29333", "IB_SH1" -> "29334", "FAC.ART_H1.05" -> "21428", "LL_H0.83" -> "21349", "WM_0.06" -> "19670",
		"WM_1.10" -> "46052", "WM_1.09" -> "46053", "WM_1.08" -> "46054", "WM_2.23" -> "46054", "WM_2.21" -> "19816", "WM_2.19" -> "19817", "WM_1.15" -> "46084",
		"WM_1.14" -> "46085", "WM_1.12" -> "46050", "WM_1.11" -> "46051", "PX_P3.31" -> "35570", "PX_P3.30" -> "35577", "PX_P3.29" -> "35564",
		"PX_P3.27" -> "35565", "PX_P3.26" -> "35572", "PX_P3.22" -> "35586", "PX_P3.20" -> "35573", "WM_2.50" -> "19844", "WM_2.49" -> "19843", "WM_2.46" ->
			"19836", "PX_PS0.16c" -> "27436", "PX_P4.33" -> "35502", "PX_P3.47" -> "35589", "PX_P3.46" -> "35587", "PX_P3.36" -> "35561", "PX_P3.32" -> "35569",
		"LB_SEMINAR" -> "39110", "LF_B0.07" -> "36984", "LF_B0.02" -> "36985", "BS_BSR3" -> "37212", "LF_D1.37" -> "35399", "ES_D1.02" -> "30944",
		"ES_D1.01" -> "30943", "LL_H0.82" -> "21374", "LF_D1.41" -> "35398", "MO_M3.01" -> "38566", "WM_L0.09" -> "47897", "WM_L0.10" -> "47868",
		"WM_L0.12" -> "47879", "WM_L0.11" -> "47892", "WM_S0.12" -> "47879", "WM_S0.14" -> "47895", "WM_S0.13" -> "47894", "WM_S0.11" -> "47892",
		"WM_0.08" -> "26803", "WM_S0.21" -> "47875", "WM_S0.20" -> "47902", "WM_S0.19" -> "47901", "WM_S0.18" -> "47900", "WM_S0.17" -> "47865",
		"WM_S0.16" -> "47864", "WM_S0.15" -> "47896", "OC0.01" -> "52134", "OC1.01" -> "52205", "OC1.09" -> "41920", "OC1.04" -> "52175", "OC1.06" -> "52178",
		"OC0.05" -> "52131", "OC1.02" -> "52173", "OC1.03" -> "52174", "OC1.07" -> "52177", "OC1.08" -> "52176", "OC1.05" -> "52209", "MD_MSBA1.30" -> "37149",
		"MD_MSBA0.39" -> "36944", "ES_SCARMAN" -> "23880", "ET_S1.102" -> "37963", "ES_D0.04" -> "30650", "LL_H0.72" -> "21357"
	)


	def parseXml(
		xml: Elem,
		year: AcademicYear,
		uniId: String,
		locationFetchingService: LocationFetchingService,
		moduleAndDepartmentService: ModuleAndDepartmentService,
		userLookup: UserLookupService
	): Seq[TimetableEvent] = {
		val moduleCodes = (xml \\ "module").map(_.text.toLowerCase).distinct
		if (moduleCodes.isEmpty) logger.info(s"No modules returned for: $uniId")
		val moduleMap = moduleAndDepartmentService.getModulesByCodes(moduleCodes).groupBy(_.code).mapValues(_.head)
		xml \\ "Activity" map { activity =>
			val name = (activity \\ "name").text

			val startTime = new LocalTime((activity \\ "start").text)
			val endTime = new LocalTime((activity \\ "end").text)

			val location = (activity \\ "room").text match {
				case text if !text.isEmpty =>
					// try and get the location from the map of managed rooms without calling the api. fall back to searching for this room
					CentrallyManagedRooms.get(text).map(MapLocation(text, _))
						.orElse({
							// S+ has some (not all) rooms as "AB_AB1.2", where AB is a building code
							// we're generally better off without this when searching.
							val removeBuildingNames = "^[^_]*_".r
							Some(locationFetchingService.locationFor(removeBuildingNames.replaceFirstIn(text, "")))
						})
				case _ => None
			}

			val parent = TimetableEvent.Parent(moduleMap.get((activity \\ "module").text.toLowerCase))

			val dayOfWeek = DayOfWeek.apply((activity \\ "day").text.toInt + 1)

			val uid =
				DigestUtils.md5Hex(
					Seq(
						name,
						startTime.toString,
						endTime.toString,
						dayOfWeek.toString,
						location.fold("") {
							_.name
						},
						parent.shortName.getOrElse(""),
						(activity \\ "weeks").text
					).mkString
				)

			TimetableEvent(
				uid = uid,
				name = name,
				title = (activity \\ "title").text,
				description = (activity \\ "description").text,
				eventType = TimetableEventType((activity \\ "type").text),
				weekRanges = new WeekRangeListUserType().convertToObject((activity \\ "weeks").text),
				day = dayOfWeek,
				startTime = startTime,
				endTime = endTime,
				location = location,
				comments = Option((activity \\ "comments").text).flatMap {
					_.maybeText
				},
				parent = parent,
				staff = userLookup.getUsersByWarwickUniIds((activity \\ "staffmember") map {
					_.text
				}).values.collect { case FoundUser(u) => u }.toSeq,
				students = userLookup.getUsersByWarwickUniIds((activity \\ "student") map {
					_.text
				}).values.collect { case FoundUser(u) => u }.toSeq,
				year = year,
				relatedUrl = None,
				attendance = Map()
			)
		}
	}
}