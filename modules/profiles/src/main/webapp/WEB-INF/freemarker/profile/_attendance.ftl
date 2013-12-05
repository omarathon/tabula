<section id="attendance" class="clearfix" >
	<div class="pull-right">
		<form class="form-inline">
			<label>
				Academic year
				<select class="academicYear input-small">
					<#if studentCourseDetails.freshStudentCourseYearDetails?? >
					<#list studentCourseDetails.freshStudentCourseYearDetails as studentCourseYearDetail>
						<#if (studentCourseYearDetail.academicYear.startYear?c)??>
							<option
								value="${studentCourseYearDetail.academicYear.startYear?c}"
								<#if studentCourseDetails.latestStudentCourseYearDetails.id == studentCourseYearDetail.id>selected</#if>
							>
								${studentCourseYearDetail.academicYear.toString}
							</option>
						</#if>
					</#list>
					</#if>
				</select>
			</label>
		</form>
	</div>
	<h4>Attendance</h4>
	<div class="monitoring-points"></div>
	<div class="small-groups"></div>
	<script type="text/javascript">
		jQuery(function($){
			var monitoringPointsLoader = function() {
				$('#attendance .monitoring-points').empty();
				$('#attendance .small-groups').empty();

				$.get('/attendance/profile/${profile.universityId}/'
						+ $('#attendance select.academicYear :selected').val()
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
					}
				});

				$.get('/groups/student/${profile.universityId}/attendance/' + $('#attendance select.academicYear :selected').val() + '?dt=' + new Date().valueOf(), function(data) {
					$('#attendance .small-groups').hide().html(data);
					var pane = $('#attendance-pane');
					if ($('#attendance .small-groups').find('h4').length == 0) {
						$('#attendance .small-groups').show();
						pane.show();
						window.GlobalScripts.initCollapsible();
					}
				});
			}
			$('#attendance select.academicYear').on('change', monitoringPointsLoader);
			monitoringPointsLoader();
		});
	</script>
</section>