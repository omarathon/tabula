<#import "*/group_components.ftl" as components />
<#escape x as x?html>
<#if nonempty(memberGroupsetModules.moduleItems) || user.student>
	<div class="header-with-tooltip" id="my-groups">
		<h2 class="section">My groups</h2>
		<span class="use-tooltip" data-toggle="tooltip" data-html="true" data-placement="bottom" data-title="Talk to your module convenor if you think a seminar, lab or tutorial is missing - maybe it isn't set up yet, or they aren't using Tabula. Please make sure that you select an assessment component when you register for modules in eMR.">Missing a group?</span>
	</div>

	<#if nonempty(memberGroupsetModules.moduleItems) >
		<div id="student-groups-view">
			<@components.module_info memberGroupsetModules />
		</div><!--student-groups-view-->
	<#else>
		<div class="alert alert-block alert-info">
			There are no groups to show you right now
		</div>
	</#if>
</#if>
</#escape>
