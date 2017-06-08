<#import "*/coursework_components.ftl" as components />
<#assign stages = command.previousMarkerFeedback?keys />
<#list stages as stage>
	<#assign markerFeedback = mapGet(command.previousMarkerFeedback, stage) />
	<div role="tabpanel" class="tab-pane previous-marker-feedback" id="${student.userId}${command.stage.name}${stage.name}">
		<@components.marker_feedback_summary markerFeedback stage command.stage command.currentMarkerFeedback />
	</div>
</#list>