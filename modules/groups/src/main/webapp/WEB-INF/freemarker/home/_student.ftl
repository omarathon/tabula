<#import "../group_components.ftl" as components />
<#if nonempty(memberGroupsetModules.moduleItems) >
<div id="student-groups-view">
		<h2>My groups</h2>
		<@components.module_info memberGroupsetModules />
</div><!--student-groups-view-->
</#if>
