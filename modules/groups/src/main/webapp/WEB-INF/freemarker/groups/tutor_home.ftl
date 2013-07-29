<#import "../group_components.ftl" as components />
<#escape x as x?html>

<h1>My small groups</h1>

<#if updatedOccurrence??>
<div class="alert alert-success">
	<button type="button" class="close" data-dismiss="alert">&times;</button>
    Attendance at 
    	${updatedOccurrence.event.group.groupSet.name}, 
		${updatedOccurrence.event.group.name}: 
		${updatedOccurrence.event.day.name} <@fmt.time updatedOccurrence.event.startTime /> - <@fmt.time updatedOccurrence.event.endTime /> 
		in <strong>Week ${updatedOccurrence.week}</strong> has been recorded.
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