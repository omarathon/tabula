<#macro admin_href submission>
	<@url page='/admin/module/${submission.assignment.module.code}/assignments/${submission.assignment.id}/submissionsandfeedback/list' />
</#macro>

<#if expired??>
	<tbody>
		<tr><td>
			<p class="alert alert-error">Sorry, this view is out of date. Please refresh the page.</p>
		</td></tr>
	</tbody>
	
	<script type="text/javascript">
		(function ($) {
			$("#activity-fetcher").remove();
		})(jQuery);
	</script>
<#else>
	<tbody>
		<#list activities.activities as activity>
			<tr>
				<td>
					<#if async??><div class="streaming" style="display:none;"></#if>
					<div class="pull-right"><@fmt.date date=activity.date at=true /></div>
					
					<div>
						<#if activity.entityType == "Submission">
							<a href="<@admin_href activity.entity />"><b>${activity.title}</b> by ${activity.agent.warwickId}</a>
		
							<#if activity.entity.late>
								<span class="label-red">Late</span>
							<#elseif activity.entity.authorisedLate>
								<span class="label-blue">Authorised Late</span>
							</#if>
							<#if activity.entity.suspectPlagiarised>
								<span class="label-orange">Suspect Plagiarised</span>
							</#if>
						<#else>
							<#-- default -->
							<b>${activity.title}</b> by ${activity.agent.warwickId}
						</#if>
					</div>
					
					<div class="activity-message">
						<#if activity.entityType == "Submission">
							<@fmt.assignment_name activity.entity.assignment />
						<#elseif activity.entityType == "Assignment">
							<@fmt.assignment_name activity.entity />
						<#else>
							${activity.message}
						</#if>
					</div>
					<#if async??></div></#if>
				</td>
			</tr>
		</#list>
	</tbody>
	
	<script type="text/javascript">
		(function ($) {
			$("#activities").data("url", "<@url page="/api/activity/pagelet/${activities.tokens}" />");
		})(jQuery);
	</script>
</#if>