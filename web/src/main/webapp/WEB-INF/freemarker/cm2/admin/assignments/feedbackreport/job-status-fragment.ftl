<div id="job-status-value" data-progress="${job.progress}" data-succeeded="${job.succeeded?string}" data-finished="${job.finished?string}">
  <p>
    <#if job.finished && !job.succeeded>
      An error occurred while generating this report. Please try again shortly. If you see this message repeatedly, please contact us using the "Need help?" button above.
    </#if>
    ${(job.status!'Getting ready...')?replace('\n','<br>')}
  </p>
</div>
