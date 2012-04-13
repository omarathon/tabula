<h2>Feedback for ${user.universityId}</h2>
	
<#if features.collectRatings>
	<div id="feedback-rating-container" class="is-stackable">
		<!-- fallback for noscript -->
		<div style="padding:0.5em">
		<a target="_blank" href="<@routes.ratefeedback feedback />">Rate your feedback</a> (opens in a new window/tab)
		</div>
	</div>
</#if>

<p>
	<#-- Only offer a Zip if there's more than one file. -->
	<#if feedback.attachments?size gt 1>
		<p>Your feedback consists of ${feedback.attachments?size} files.</p>
		<p>
			<a href="<@url page="/module/${module.code}/${assignment.id}/all/feedback.zip"/>">
				Download all as a Zip file
			</a>
		</p>
		<p>Or download the attachments individually below.</p>
	<#else>
		<p>Your feedback file is available to download below.</p>
	</#if>
	
	<ul class="file-list">
	<#list feedback.attachments as attachment>
		<li>
		<a href="<@url page="/module/${module.code}/${assignment.id}/get/${attachment.name?url}"/>">
			${attachment.name}
		</a>
		</li>
	</#list>
	</ul>
</p>