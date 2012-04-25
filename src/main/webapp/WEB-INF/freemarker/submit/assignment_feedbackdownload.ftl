<h2>Feedback for ${user.universityId}</h2>
	
<#if features.collectRatings && feedback.collectRatings>
	<div id="feedback-rating-container" class="is-stackable">
		<!-- fallback for noscript -->
		<div style="padding:0.5em">
		<a target="_blank" href="<@routes.ratefeedback feedback />">Rate your feedback</a> (opens in a new window/tab)
		</div>
	</div>
</#if>

<p>
	<#assign feedbackcount=feedback.attachments?size>
	<#-- Only offer a Zip if there's more than one file. -->
	<#if feedbackcount gt 1>
		<p>Your feedback consists of ${feedback.attachments?size} files.</p>
		<p>
			<a class="btn btn-success" href="<@url page="/module/${module.code}/${assignment.id}/all/feedback.zip"/>"><i class="icon-gift"></i>
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
		<a class="btn<#if feedbackcount=1> btn-success</#if>" href="<@url page="/module/${module.code}/${assignment.id}/get/${attachment.name?url}"/>"><i class="icon-file"></i>
			${attachment.name}
		</a>
		</li>
	</#list>
	</ul>
</p>