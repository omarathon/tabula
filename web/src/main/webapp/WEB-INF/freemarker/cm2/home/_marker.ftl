<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<#if !embedded>
	<h1>Assignments for marking</h1>

	<#if !markerInformation??>
		<div class="marker-information">
			<p class="hint">Loading&hellip;</p>
		</div>

		<script type="text/javascript">
			(function ($) {
				$('.marker-information').load('<@routes.cm2.markerHome />');
			})(jQuery);
		</script>
	</#if>
</#if>

<#if markerInformation??>
	<@components.marker_assignment_list id="marker-action" title="Action required" assignments=markerInformation.actionRequiredAssignments verb="Mark" show_actions=isSelf!true />
	<@components.marker_assignment_list id="marker-noaction" title="No action required" assignments=markerInformation.noActionRequiredAssignments verb="Review" expand_by_default=(!markerInformation.actionRequiredAssignments?has_content) show_actions=isSelf!true />
	<@components.marker_assignment_list id="marker-upcoming" title="Upcoming" assignments=markerInformation.upcomingAssignments verb="" expand_by_default=(!markerInformation.actionRequiredAssignments?has_content && !markerInformation.noActionRequiredAssignments?has_content) show_actions=isSelf!true />
	<@components.marker_assignment_list id="marker-completed" title="Completed" assignments=markerInformation.completedAssignments verb="Review" expand_by_default=(!markerInformation.actionRequiredAssignments?has_content && !markerInformation.noActionRequiredAssignments?has_content && !markerInformation.upcomingAssignments?has_content) show_actions=isSelf!true />

	<script type="text/javascript">
		(function ($) {
			$('.use-tooltip').tooltip();
			GlobalScripts.initCollapsible();
		})(jQuery);
	</script>
</#if>

</#escape>