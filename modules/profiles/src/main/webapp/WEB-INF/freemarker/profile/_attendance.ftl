<section id="attendance" class="clearfix" >
	<#assign year=studentCourseYearDetails.academicYear?? />

	<h4>Attendance</h4>
	<div class="monitoring-points"></div>
	<div class="small-groups"></div>
	<script type="text/javascript">
		jQuery(function($){
			var monitoringPointsLoader = function() {
				$('#attendance .monitoring-points').empty();
				$('#attendance .small-groups').empty();

				$.get('/attendance/profile/${profile.universityId}/{year}'
						+ '?dt=' + new Date().valueOf()
						+ '&expand=' + (window.location.search.indexOf('updatedMonitoringPoint') >= 0)
				, function(data) {
					$('#attendance .monitoring-points').html(data);
					var pane = $('#attendance-pane');
					var title = pane.find('h4').html();
					if (title != '' && title != undefined) {
						pane.find('.title').html(title);
						$('#attendance-pane').show();
						window.GlobalScripts.initCollapsible();
						$('.use-tooltip').tooltip();
						$('.use-popover').tabulaPopover({
							trigger: 'click',
							container: '#container'
						});
					}
				});

				$.get('/groups/student/${profile.universityId}/attendance/{year}' + '?dt=' + new Date().valueOf(), function(data) {
					$('#attendance .small-groups').hide().html(data);
					var pane = $('#attendance-pane');
					if ($('#attendance .small-groups').find('.seminar-attendance-profile').length > 0) {
						$('#attendance .small-groups').show();
						pane.show();
						window.GlobalScripts.initCollapsible();
						$('.use-tooltip').tooltip();
						$('.use-popover').tabulaPopover({
							trigger: 'click',
							container: '#container'
						});
					}
				});
			};
			monitoringPointsLoader();
		});
	</script>
</section>
