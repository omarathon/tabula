<#assign jobId=job.id/>

<h1>Feedback report</h1>

<p>This page will update itself automatically. You'll be sent an email when it completes so you don't have to keep this page open.</p>

<div id="job-status-fragment" class="well">
	<#include "job-status-fragment.ftl" />
</div>

<div id="job-progress">
	<div class="progress progress-striped active">
		<div class="bar" style="width: 100%;"></div>
	</div>
</div>
