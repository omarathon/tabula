<#if info.ajax>

	<#include "job-status-fragment.ftl" />

<#else>

	<h1>Job status</h1>
	
	<p>Job ID ${jobId}</p>
	
	<div id="job-status-fragment">
	<#include "job-status-fragment.ftl" />
	</div>
	
	<script>
	(function($){
	
	var $fragment = $('#job-status-fragment');
	var updateFragment = function() {
		$fragment.load(${url('/sysadmin/jobs/job-status')}, {id: '${jobId}'}, function(){
			setTimeout(updateFragment, 2000);
		});
	};
	setTimeout(updateFragment, 2000);
	
	})(jQuery);
	</script>

</#if>