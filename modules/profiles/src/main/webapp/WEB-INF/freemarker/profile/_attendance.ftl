<section id="attendance" class="clearfix" >
	<div class="pull-right">
		<form class="form-inline">
			<label>
				Academic year
				<select class="academicYear input-small">
					<#list studentCourseDetails.studentCourseYearDetails as studentCourseYearDetail>
						<option
							value="${studentCourseYearDetail.academicYear.startYear?c}"
							<#if studentCourseDetails.latestStudentCourseYearDetails.id == studentCourseYearDetail.id>selected</#if>
						>
							${studentCourseYearDetail.academicYear.toString}
						</option>
					</#list>
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
				
				$.get('/attendance/profile/${studentCourseDetails.urlSafeId}/' + $('#attendance select.academicYear :selected').val() + '?dt=' + new Date().valueOf(), function(data) {
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
					$('#attendance .small-groups').html(data);
					var pane = $('#attendance-pane');
					$('#attendance-pane').show();
					window.GlobalScripts.initCollapsible();
				});
			}
			$('#attendance select.academicYear').on('change', monitoringPointsLoader);
			monitoringPointsLoader();
		});
	</script>
</section>