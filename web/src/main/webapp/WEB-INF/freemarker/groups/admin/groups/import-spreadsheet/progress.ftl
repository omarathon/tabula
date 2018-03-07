<#escape x as x?html>
	<#if info.ajax>
		<#include "job-status-fragment.ftl" />
	<#else>
		<h1>Import small groups from spreadsheet</h1>

		<p>This page updates automatically.</p>

		<div id="job-status-fragment" class="well">
			<#include "job-status-fragment.ftl" />
		</div>

		<div id="job-progress">
			<div class="progress">
				<div class="progress-bar progress-bar-striped active" style="min-width: 5%; width: ${job.progress}%;"></div>
			</div>
		</div>

		<p>
			<a href="<@routes.groups.departmenthome department academicYear />" class="btn btn-default">Return to Small Group Teaching</a>
		</p>

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
