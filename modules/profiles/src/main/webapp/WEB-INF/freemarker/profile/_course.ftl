<section id="course" >
	<div class="student-course">
		<div class="clearfix">
			<div class="btn-group student-course-choice-button pull-right">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown">
					Other course records
			   </a>
				<ul class="dropdown-menu">
					<#list profile.studentCourseDetails as courseDetails>
						<#if courseDetails.scjCode != studentCourseDetails.scjCode>
							<li>
								<a role="presentation"><a role="menuitem" tabindex="-1" href="/view/course/${courseDetails.scjCode}">
									<@fmt.dropdown_name courseDetails  />
								</a>
							</li>
						</#if>
					</#list>
				</ul>
			</div>
			<div class="course-name">
				<h2>
					Course: ${(studentCourseDetails.course.name)!} (${(studentCourseDetails.course.code?upper_case)!})
				</h2>
			</div>
			<div class="course-year">
				<h2 class="muted">${studentCourseDetails.beginYear} to ${studentCourseDetails.endYear}</h2>
			</div>
		</div>
	</div>


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

			<#if (features.profilesMemberNotes && can.do('MemberNotes.Read', profile)) >
				<li id="membernote-pane">
					<#include "_member_notes.ftl" />
				</li>
			</#if>

			<#list (studentCourseDetails.department.displayedStudentRelationshipTypes)![] as relationshipType>
				<#if studentCourseDetails.hasRelationship(relationshipType) || relationshipType.displayIfEmpty(studentCourseDetails)>
					<li id="${relationshipType.id}-pane">
						<#assign relMeetings=(meetingsById[relationshipType.id])![] />
						<@profile_macros.relationship_section studentCourseDetails relationshipType relMeetings />
					</li>
				</#if>
			</#list>

			<#if numSmallGroups gt 0>
				<li id="sg-pane" style="display:none;">
					<#include "_small_groups.ftl" />
				</li>
			</#if>

			<#if studentCourseDetails.hasModuleRegistrations>
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
</section>