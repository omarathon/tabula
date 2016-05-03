<#import "*/modal_macros.ftl" as modal />

<#escape x as x?html>

<!-- nav to choose other years: -->
<#if (studentCourseDetails.freshStudentCourseYearDetails)??>

	<ul class="nav nav-tabs nav-justified">
		<#list studentCourseDetails.freshStudentCourseYearDetails as scyd>
			<#assign year=scyd.academicYear.startYear?string.computer />
			<#if scyd.academicYear.value != studentCourseYearDetails.academicYear.value>
				<li><a href="/profiles/view/course/${studentCourseDetails.urlSafeId}/${year}">
					${scyd.academicYear.toString}
				</a></li>
			<#else>
				<li class="active"><a href="/profiles/view/course/${studentCourseDetails.urlSafeId}/${year}">
					${scyd.academicYear.toString}
				</a></li>
			</#if>
		</#list>
	</ul>
</#if>

<#if studentCourseYearDetails??>
	<!-- course year details  -->
	<div class="data clearfix">
		<div id="course-in-year-info">
			<table class="profile-or-course-info">
				<tbody>
					<#if !isSelf && studentCourseYearDetails.enrolmentStatus??>
						<tr>
							<th>Enrolment status</th>
							<td><@fmt.enrolment_status studentCourseYearDetails />
							</td>
						</tr>
					</#if>
					<#if studentCourseYearDetails.modeOfAttendance??>
						<tr>
							<th>Attendance</th>
							<td>${(studentCourseYearDetails.modeOfAttendance.fullNameAliased)!}
							</td>
						</tr>
					</#if>
					<#if studentCourseYearDetails.yearOfStudy??>
						<tr>
							<th>Year of study</th>
							<td>${(studentCourseYearDetails.yearOfStudy)!}
							</td>
						</tr>
					</#if>
				</tbody>
			</table>
		</div>
	</div>

	<#assign defaultView = "gadget" />
	<#if user?? && userSetting('profilesDefaultView')?has_content>
		<#assign defaultView = userSetting('profilesDefaultView') />
	</#if>
	<div class="tabbable" data-default-view="${defaultView}">

	<#assign showTimetablePane=features.personalTimetables
		&& can.do("Profiles.Read.Timetable", profile)
		&& profile.defaultYearDetails.equals(studentCourseYearDetails) />

	<#if showTimetablePane>
		<script type="text/javascript">
			var weeks = ${weekRangesDumper()}
		</script>
	</#if>
		<ol class="panes">

			<#if showTimetablePane>
				<@profile_macros.timetablePane studentCourseDetails.student />
			</#if>

			<#list (allRelationshipTypes)![] as relationshipType>
				<#if studentCourseDetails.hasRelationship(relationshipType) || relationshipType.displayIfEmpty(studentCourseDetails) && studentCourseDetails.isStudentRelationshipTypeForDisplay(relationshipType)>
					<li id="${relationshipType.id}-pane">
						<@profile_macros.relationship_section studentCourseDetails relationshipType />
					</li>
				</#if>
			</#list>

			<#if features.courseworkInStudentProfile>
				<li id="coursework-pane" data-title="Coursework">
					<#include "_coursework.ftl" />
				</li>
			</#if>

			<li id="sg-pane" data-title="Groups">
				<#include "_small_groups.ftl" />
			</li>

			<#if studentCourseYearDetails??>
				<li id="module-registration-pane" data-title="Modules">
					<#include "_module_registrations.ftl" />
				</li>
			</#if>

			<#if features.attendanceMonitoring>
				<li id="attendance-pane" data-title="Attendance">
					<#include "_attendance.ftl" />
				</li>
			</#if>
		</ol>

		<div id="note-modal" class="modal fade">
			<@modal.wrapper>
				<@modal.header>
					<h3 class="modal-title"><span></span> for ${profile.fullName}</h3>
				</@modal.header>
				<@modal.body></@modal.body>
				<@modal.footer>
					<input id="member-note-save" type="submit" class="btn btn-primary" value="Save">
				</@modal.footer>
			</@modal.wrapper>
		</div>

		<div id="modal" class="modal fade" style="display:none;"></div>

		<div id="modal-change-agent" class="modal fade"></div>

		<script type="text/javascript">
			jQuery(function($){
				// load edit personal agent
				$(".relationship-section").on("click", ".edit-agent-link, .add-agent-link", function(e) {
					e.preventDefault();
					var url = $(this).attr('href');

					// TAB-1111 we pass the second arg as a string, not an object, because if you use an object
					// it makes it a POST request
					$("#modal-change-agent").load(url, 'ts=' + new Date().getTime(),function(){
						$("#modal-change-agent").modal('show');
					});
				});
			});
		</script>
	</div>
</#if>

</#escape>