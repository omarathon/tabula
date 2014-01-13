<#include "_course_details.ftl" />

<div class="tabbable">
	<#assign showTimetablePane=features.personalTimetables && can.do("Profiles.Read.Timetable", profile) />

	<#if showTimetablePane>
		<script type="text/javascript">
			var weeks = ${weekRangesDumper()}
		</script>
	</#if>
	<ol class="panes">

		<#if showTimetablePane>
			<li id="timetable-pane">
				<section id="timetable-details" class="clearfix" >
					<h4>Timetable</h4>
					<div class='fullCalendar' data-viewname='agendaWeek' data-studentid='${studentCourseDetails.student.universityId}'>
					</div>
    			</section>
			</li>
		</#if>

		<#list (allRelationshipTypes)![] as relationshipType>
			<#if studentCourseDetails.hasRelationship(relationshipType) || relationshipType.displayIfEmpty(studentCourseDetails) && studentCourseDetails.department.isStudentRelationshipTypeForDisplay(relationshipType)>
				<li id="${relationshipType.id}-pane">
					<#assign relMeetings=(meetingsById[relationshipType.id])![] />
					<@profile_macros.relationship_section studentCourseDetails relationshipType relMeetings />
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

		<#if studentCourseDetails.hasModuleRegistrations && studentCourseDetails.latestStudentCourseYearDetails??>
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
