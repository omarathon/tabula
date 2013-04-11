<#if info.ajax>

	<#include "job-status-fragment.ftl" />

<#else>

	<#assign jobId=job.id/>

	<h1>Job status</h1>
	
	<p>This page will update itself automatically. You'll be sent an email when it completes so you don't have to keep this page open.</p>
	
	<#if assignment?? >
		<p>When the job is finished you'll be able to see the results on the 
		<a href="<@routes.assignmentsubmissionsandfeedback assignment />">submissions page</a>.</p>
	</#if>
	
	<!-- <p>Job ID ${jobId}</p> -->
	
	<div id="job-status-fragment" class="well">
	<#include "job-status-fragment.ftl" />
	</div>
	
	<div id="job-progress">
		<div class="progress progress-striped active">
		  <div class="bar" style="width: ${5 + job.progress*0.95}%;"></div>
		</div>
	</div>
	
	<script>
	(function($){
	
	var updateProgress = function() {
		// grab progress from data-progress attribute in response.
		var $value = $('#job-status-value');
		var $progress = $('#job-progress .progress');
		var percent = $value.data('progress');
		$progress.find('.bar').css('width', Math.floor(5+(percent*0.95))+'%');
		if ($value.data('finished')) {
			$progress.removeClass('active');
			if ($value.data('succeeded') == false) {
				$progress.addClass('progress-warning');
			} else {
				$progress.addClass('progress-success');
			}
		} else {
			setTimeout(updateFragment, 2000);
		}
	}
	
	var $fragment = $('#job-status-fragment');
	var updateFragment = function() {
		$fragment.load('?ajax&jobId=${jobId}', function(){
			updateProgress();
		});
	};
	setTimeout(updateFragment, 2000);
	updateProgress();
	
	})(jQuery);
	</script>

</#if>