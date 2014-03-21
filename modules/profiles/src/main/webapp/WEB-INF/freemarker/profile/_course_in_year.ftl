<#import "*/modal_macros.ftl" as modal />

<!-- nav to choose other years: -->
<#if (studentCourseDetails.freshStudentCourseYearDetails)?? && (studentCourseDetails.freshStudentCourseYearDetails?size > 1)>

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

<!-- course year details  -->
<div class="data clearfix">
	<div id="course-in-year-info">
		<table class="profile-or-course-info">
			<tbody>
				<#if !isSelf>
					<tr>
						<th>Enrolment status</th>
						<td><@fmt.enrolment_status studentCourseYearDetails />
						</td>
					</tr>
				</#if>
				<tr>
					<th>Attendance</th>
					<td>${(studentCourseYearDetails.modeOfAttendance.fullNameAliased)!}
					</td>
				</tr>
				<tr>
					<th>Year of study</th>
					<td>${(studentCourseYearDetails.yearOfStudy)!}
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</div>


<div class="tabbable">

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
			<li id="timetable-pane">
				<section id="timetable-details" class="clearfix" >
					<h4>
						Timetable
						<#if studentCourseDetails.student.timetableHash?has_content>
							<a href="<@routes.timetable_ical studentCourseDetails.student />" title="Subscribe to timetable">
								<i class="icon-calendar"></i>
							</a>
						</#if>
					</h4>
					<div class='fullCalendar' data-viewname='agendaWeek' data-studentid='${studentCourseDetails.student.universityId}'>
					</div>
				</section>
			</li>

			<#if studentCourseDetails.student.timetableHash?has_content>
				<div class="modal hide fade" id="timetable-ical-modal">
					<@modal.header>
						<h2>Subscribe to your timetable</h2>
					</@modal.header>
					<@modal.body>
						<#if isSelf>
							<div class="alert alert-info">
								<p>Tabula provides your timetable as a calendar feed with a "private address". Private Addresses are designed for your use only. They don't require any further authentication to get information from your timetable, so they're useful for getting your timetable into another calendar or application, or your mobile phone.</p>
								<p>If you accidentally share the address with others, you can change the address by clicking the button below. All of the existing clients using this private address will break, and you will have to give them the new private address.</p>
								<form class="form-inline" method="POST" action="<@routes.timetable_ical_regenerate />"><button type="submit" class="btn btn-primary">Generate a new private address</button></form>
							</div>
						</#if>

						<p>You can <a href="<@routes.timetable_ical studentCourseDetails.student />">click this link</a> to subscribe to your timetable in your default calendar application.</p>

						<p>You can also copy the link and paste it into an external application, e.g. Google Calendar:</p>

						<p><a href="<@routes.timetable_ical studentCourseDetails.student />"><@routes.timetable_ical studentCourseDetails.student false /></a></p>

					</@modal.body>
				</div>
			</#if>
		</#if>

		<#list (allRelationshipTypes)![] as relationshipType>
			<#if studentCourseDetails.hasRelationship(relationshipType) || relationshipType.displayIfEmpty(studentCourseDetails) && studentCourseDetails.department.isStudentRelationshipTypeForDisplay(relationshipType)>
				<li id="${relationshipType.id}-pane">
					<#assign relMeetings=(meetingsById[relationshipType.id])![] />
						<@profile_macros.relationship_section studentCourseDetails relationshipType relMeetings viewerRelationshipTypes />
				</li>
			</#if>
		</#list>

		<#if features.courseworkInStudentProfile && can.do("Profiles.Read.Coursework", profile)>
			<li id="coursework-pane" style="display:none;">
				<#include "_coursework.ftl" />
			</li>
		</#if>

		<#if numSmallGroups gt 0>
			<li id="sg-pane" style="display:none;">
				<#include "_small_groups.ftl" />
			</li>
		</#if>

		<#if studentCourseDetails.hasModuleRegistrations && studentCourseYearDetails??>
			<li id="module-registration-pane">
				<#include "_module_registrations.ftl" />
			</li>
		</#if>

		<#if features.attendanceMonitoring>
			<li id="attendance-pane" style="display:none;">
				<#include "_attendance.ftl" />
			</li>
		</#if>
	</ol>

	<div id="note-modal" class="modal hide fade">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
			<h3><span></span> for ${profile.fullName}</h3>
		</div>
		<div class="modal-body"></div>

		<div class="modal-footer">
			<input id="member-note-save" type="submit" class="btn btn-primary" value="Save">
		</div>
	</div>

	<div id="modal" class="modal hide fade" style="display:none;"></div>

	<div id="modal-change-agent" class="modal hide fade"></div>

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
