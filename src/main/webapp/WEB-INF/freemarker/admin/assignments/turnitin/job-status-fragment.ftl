<div id="job-status-value" data-progress="${job.progress}" data-succeeded="${job.succeeded?string}" data-finished="${job.finished?string}">
<p>
${(job.status!'Status unknown.')?replace('\n','<br>')}
</p>
</div>