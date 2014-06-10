<script>
	var weeks = ${weekRangesDumper()}
</script>

<#import "../related_students/related_students_macros.ftl" as relationships />

<div class="tabbable">

	<ol class="panes">
		<#-- The url for staff only shows groups for current user - if staff profiles become viewable by more people this needs to change -->
		<#if  (smallGroups?size > 0)>
			<li id="sg-pane" style="display:none;" data-title="Groups">
				<#assign groupsWidgetUrl = '/groups/tutor/' />
				<#include "_small_groups.ftl" />
			</li>
		</#if>

		<li id="timetable-pane" data-title="Timetable">
			<section id="timetable-details" class="clearfix" >
				<h4>Timetable
					<#if profile.timetableHash?has_content>
						<a href="<@routes.timetable_ical profile />" title="Subscribe to timetable"><i class="icon-calendar"></i></a>
	
						<span class="subscribe">
							<a href="<@routes.timetable_ical profile />" title="Subscribe to timetable">
								Subscribe
							</a>
						</span>
					</#if>
				</h4>
				<div class='fullCalendar' data-viewname='agendaWeek' data-studentid='${profile.universityId}'></div>
			</section>
		</li>

		<#if (viewerRelationshipTypes?size > 0)>
			<li id="attendance-pane" data-title="Monitoring Points">
				<section id="attendance-details" class="clearfix" >
					<h4>Monitoring Points</h4>
					<ul>
					<#list viewerRelationshipTypes as relationshipType>
						<li><h5><a id="relationship-${relationshipType.urlPart}" href="<@routes.agentHomeForYear relationshipType '2013' />">${relationshipType.studentRole?cap_first}s 13/14</a></h5></li>
						<#if features.attendanceMonitoringAcademicYear2014>
							<li><h5><a id="relationship-${relationshipType.urlPart}" href="<@routes.agentHomeForYear relationshipType '2014'/>">${relationshipType.studentRole?cap_first}s 14/15</a></h5></li>
						</#if>
					</#list>
					</ul>
				</section>
			</li>

			<li id="relationships-pane" data-title="My Students">
				<@relationships.myStudents viewerRelationshipTypes smallGroups />
			</li>
		</#if>

		<li id="coursework-pane" data-title="My Marking">
			<#include "_marking.ftl" />
		</li>
	</ol>

</div>