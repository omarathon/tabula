<#import "*/modal_macros.ftl" as modal />
<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<#if info.ajax>
		<#include "job-status-fragment.ftl" />
	<#else>
		<@cm2.headerMenu department />

		<#function route_function dept>
			<#local result><@routes.cm2.feedbackreport dept /></#local>
			<#return result />
		</#function>
		<@fmt.id7_deptheader "Feedback report status" route_function "for" />

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
						$bar.removeClass('active');
						if ($value.data('succeeded') == false) {
							$bar.addClass('progress-bar-warning');
						} else {
							$bar.addClass('progress-bar-success');
						}
					} else {
						setTimeout(updateFragment, 2000);
					}
				}

				var $fragment = $('#job-status-fragment');
				var updateFragment = function() {
					$fragment.load('?ajax&jobId=${job.id}', function(){
						updateProgress();
					});
				};
				setTimeout(updateFragment, 2000);
				updateProgress();

			})(jQuery);
		</script>
	</#if>
</#escape>