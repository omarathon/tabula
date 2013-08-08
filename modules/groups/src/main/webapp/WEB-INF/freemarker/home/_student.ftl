<#import "../group_components.ftl" as components />
<#if nonempty(memberGroupsetModules.moduleItems) >
<div id="student-groups-view" class="row">
	<div class="span9">
        <h2>My groups</h2>
   		<@components.module_info memberGroupsetModules />

    </div><!-- span9 -->
</div><!--row-->
</#if>
