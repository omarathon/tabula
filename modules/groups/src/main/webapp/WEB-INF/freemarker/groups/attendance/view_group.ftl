<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	
	<h1>
		${group.groupSet.name}, ${group.name}
		<#if can.do("SmallGroups.ReadMembership", group)>
			<a href="<@routes.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
				<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
			</a>
		<#else>
			<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
		</#if>
	</h1>
	
	<ul class="unstyled margin-fix">
		<#list group.events as event>
			<li>
				<#if event.tutors??>
					<h6>Tutor<#if (event.tutors.size > 1)>s</#if>:
						<#if (event.tutors.size < 1)>[no tutor]</#if>
						<#list event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>
					</h6>
				</#if>
				<@components.event_schedule_info event />
			</li>
		</#list>
	</ul>
	
	<@components.singleGroupAttendance group instances studentAttendance />
	
	<#-- List of students modal -->
	<div id="students-list-modal" class="modal fade">
	</div>
</#escape>