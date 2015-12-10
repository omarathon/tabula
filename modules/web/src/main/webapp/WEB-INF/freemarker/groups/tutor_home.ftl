<#import "*/group_components.ftl" as components />
<#escape x as x?html>

<#if ajax>
	<h4>My small groups</h4>
<#else>
	<h1>My small groups</h1>
</#if>


<#if updatedOccurrence??>
<div class="alert alert-info">
	<button type="button" class="close" data-dismiss="alert">&times;</button>
    Attendance at
		<#if updatedOccurrence.event.title?has_content>${updatedOccurrence.event.title},</#if>
		${updatedOccurrence.event.group.groupSet.name},
		${updatedOccurrence.event.group.name}:
		${updatedOccurrence.event.day.name} <@fmt.time updatedOccurrence.event.startTime /> - <@fmt.time updatedOccurrence.event.endTime />
		in <strong><@fmt.singleWeekFormat week=updatedOccurrence.week academicYear=updatedOccurrence.event.group.groupSet.academicYear dept=updatedOccurrence.event.group.groupSet.module.adminDepartment /></strong> has been recorded.
</div>
</#if>

<#-- Hide while it doesn't actually do anything.
	When we do make it work, make it work for the other similar pages e.g. /groups/admin/department/{dept}/
<div class="input-prepend">
	<span class="add-on"><i class="icon-search"></i></button></span>
	<input class="span4" type="text" placeholder="Search for a module, student, group">
</div>
-->

<@components.module_info data />

</#escape>