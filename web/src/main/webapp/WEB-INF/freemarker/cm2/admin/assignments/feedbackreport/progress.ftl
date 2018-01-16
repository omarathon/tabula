<#import "*/modal_macros.ftl" as modal />
<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<#if info.ajax>
		<#include "job-status-fragment.ftl" />
	<#else>
		<#function route_function dept>
			<#local result><@routes.cm2.feedbackreport dept academicYear /></#local>
			<#return result />
		</#function>
		<@cm2.departmentHeader "Feedback report status" department route_function academicYear />

		<p>This page updates itself automatically. You'll receive an email with the report attached when it is ready, so you don't need to keep this page open.</p>

		<!-- <p>Job ID ${job.id}</p> -->

		<div id="job-status-fragment" class="well">
			<#include "job-status-fragment.ftl" />
		</div>

		<div id="job-progress">
			<div class="progress">
				<div class="progress-bar progress-bar-striped active" style="min-width: 5%; width: ${job.progress}%;"></div>
			</div>
		</div>

		<p><a href="<@routes.cm2.departmenthome department academicYear/>">Return to Assignments in ${department.name} (${academicYear.toString})</a></p>

		<script>
			(function($){

				var updateProgress = function() {
					// grab progress from data-progress attribute in response.
					var $value = $('#job-status-value');
					var $progress = $('#job-progress .progress');
					var $bar = $progress.find('.progress-bar');
					var percent = parseInt($value.data('progress'));
					$bar.css('width', Math.floor(percent) + '%');
					if ($value.data('finished')) {
						$bar.css('width', '100%');
						$bar.removeClass('active');
						if ($value.data('succeeded') === false) {
							$bar.addClass('progress-bar-danger');
						} else {
							$bar.addClass('progress-bar-success');
						}
					} else {
						setTimeout(updateFragment, 300);
					}
				};

				var $fragment = $('#job-status-fragment');
				var updateFragment = function() {
					$fragment.load('?ajax&jobId=${job.id}&ts=' + new Date().getTime(), function(){
						updateProgress();
					});
				};
				setTimeout(updateFragment, 300);
				updateProgress();

			})(jQuery);
		</script>
	</#if>
</#escape>