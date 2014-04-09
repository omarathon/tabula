<section id="coursework" class="clearfix">
	<script type="text/javascript">
		jQuery(function($){
			$('#coursework').load('/coursework/student/bycourseandyear/${studentCourseYearDetails.id}', {ts: new Date().getTime()}, function() {
				var pane = $('#coursework-pane');
				var title = pane.find('h4').first().html();
				if (title != '' && title != undefined) {
					pane.find('.title').html(title);
					window.GlobalScripts.initCollapsible();
					$('#coursework-pane').show();
				}
			});
		});
	</script>
</section>