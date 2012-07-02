<#if info.ajax>

	<#include "job-status-fragment.ftl" />

<#else>

	<h1>Job status</h1>
	
	<p>Job ID ${jobId}</p>
	
	<div id="job-status-fragment" class="well">
	<#include "job-status-fragment.ftl" />
	</div>
	
	<div id="job-progress">
		<div class="progress progress-striped active">
		  <div class="bar" style="width: 30%;"></div>
		</div>
	</div>
	
	<script>
	(function($){
	
	var $fragment = $('#job-status-fragment');
	var updateFragment = function() {
		$fragment.load('', {id: '${jobId}'}, function(){
		
			// grab progress from data-progress attribute in response.
			var percent = $('#job-status-value').data('progress');
			$('#job-progress .bar').css('width', percent+'%');
			
			setTimeout(updateFragment, 2000);
		});
	};
	setTimeout(updateFragment, 2000);
	
	})(jQuery);
	</script>

</#if>