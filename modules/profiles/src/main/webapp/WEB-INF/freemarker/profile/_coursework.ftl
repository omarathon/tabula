<section id="coursework" class="clearfix">
	<script type="text/javascript">
		jQuery(function($){
			$('#coursework').load('/coursework/student/${profile.universityId}', {ts: new Date().getTime()}, function() {
				var pane = $('#coursework-pane');
				var title = pane.find('h2').html();
				if (title != '' && title != undefined) {
					pane.find('.title').html(title);
					$('#coursework-pane').show();
				}
			});
		});
	</script>
</section>