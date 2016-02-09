<#escape x as x?html>

<script>
	var weeks = ${weekRangesDumper()}
</script>

<#import "../related_students/related_students_macros.ftl" as relationships />

<#assign defaultView = "gadget" />
<#if user?? && userSetting('profilesDefaultView')?has_content>
	<#assign defaultView = userSetting('profilesDefaultView') />
</#if>
<div class="tabbable" data-default-view="${defaultView}">

	<ol class="panes">
		<@profile_macros.timetablePane profile />

		<#-- The url for staff only shows groups for current user - if staff profiles become viewable by more people this needs to change -->
		<#if isSelf && (smallGroups?size > 0)>
			<li id="sg-pane" data-title="Groups">
				<#assign groupsWidgetUrl = '/groups/tutor/' />
				<#include "_small_groups.ftl" />
			</li>
		</#if>

		<#if isSelf && (viewerRelationshipTypes?size > 0)>
			<li id="attendance-pane" data-title="Monitoring Points">
				<section id="attendance-details" class="clearfix" >
					<h4>Monitoring Points</h4>
					<ul>
					<#list viewerRelationshipTypes as relationshipType>
						<li><h5><a id="relationship-${relationshipType.urlPart}" href="<@routes.profiles.agentHomeForYear relationshipType '2013' />">${relationshipType.studentRole?cap_first}s 13/14</a></h5></li>
						<#if features.attendanceMonitoringAcademicYear2014>
							<li><h5><a id="relationship-${relationshipType.urlPart}" href="<@routes.profiles.agentHomeForYear relationshipType '2014'/>">${relationshipType.studentRole?cap_first}s 14/15</a></h5></li>
						</#if>
						<#if features.attendanceMonitoringAcademicYear2015>
							<li><h5><a id="relationship-${relationshipType.urlPart}" href="<@routes.profiles.agentHomeForYear relationshipType '2015'/>">${relationshipType.studentRole?cap_first}s 15/16</a></h5></li>
						</#if>
					</#list>
					</ul>
				</section>
			</li>

			<li id="relationships-pane" data-title="My Students">
				<@relationships.myStudents viewerRelationshipTypes smallGroups />
			</li>
		</#if>

		<#if isSelf>
			<li id="coursework-pane" data-title="My Marking">
				<#include "_marking.ftl" />
			</li>
		</#if>
	</ol>

</div>

</#escape>