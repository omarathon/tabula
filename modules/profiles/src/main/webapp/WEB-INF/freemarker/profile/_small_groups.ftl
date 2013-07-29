<section id="small-groups" class="clearfix" >
	<script type="text/javascript">
		jQuery(function($){
			$('#small-groups').load('/groups/student', function() {
				var pane = $('#sg-pane');
				var title = pane.find('h4').html();
				pane.find('.title').html(title);
				$('a.ajax-modal', '#small-groups').ajaxModalLink();
				$('#sg-pane').show();
			});
		});
	</script>
</section>