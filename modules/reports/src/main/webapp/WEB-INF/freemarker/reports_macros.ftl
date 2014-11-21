<#macro reportLoader reportUrl>
	<div class="loading">
		<p><em>Building report...</em></p>
	
		<div class="progress">
			<div class="bar" style="width: 33%;"></div>
		</div>
	</div>
	
	<div class="complete alert alert-success" style="display: none;">
		<p>Report complete</p>
		<#nested />
	</div>
	
	<div class="alert alert-error" style="display: none;">
		<p>There was a problem generating the report. If the problem persists, please contact the <a href="mailto:webteam@warwick.ac.uk">ITS Web Team</a>.</p>
	</div>
	
	<div class="report-target"></div>
	<form class="report-target-form" style="display: none;" method="POST" action=""></form>
	
	<script>
		jQuery(function($){
			$.ajax('${reportUrl}', {
				type: 'POST',
				success: function(data) {
					var $mainContent = $('#main-content');
					$mainContent.find('.loading p em').html('Downloading data&hellip;');
					$mainContent.find('.loading .bar').width('66%');
					setTimeout(function(){
						var key, key1, key2, result = [];
						for (key in data) {
							if (data.hasOwnProperty(key)) {
								for (key1 in data[key]) {
									if (data[key].hasOwnProperty(key1)) {
										for (key2 in data[key][key1]) {
											if (data[key][key1].hasOwnProperty(key2)) {
												result.push(
														$('<input/>').prop({
															'type': 'hidden',
															'name': key + '[' + key1 + '][' + key2 + ']',
															'value': data[key][key1][key2]
														})
												);
											}
										}
									}
								}
							}
						}
	
						$(result).appendTo($('form.report-target-form'));
	
						$mainContent.find('div.loading').hide();
						$mainContent.find('div.complete').show();
	
						$mainContent.find('div.complete div.download ul.dropdown-menu a').on('click', function(e) {
							e.preventDefault();
							$('form.report-target-form').prop('action', $(this).data('href')).submit();
						});
						$mainContent.find('div.complete a.show-data').on('click', function(e) {
							e.preventDefault();
							var $this = $(this);
							$.post($this.data('href'), data, function(html) {
								$('.report-target').html(html);
								$this.hide();
							});
						});
					}, 500);
				},
				error: function() {
					$('#main-content').find('div.loading').hide().end().find('div.alert-error').show();
				}
			});
		});
	</script>
</#macro>