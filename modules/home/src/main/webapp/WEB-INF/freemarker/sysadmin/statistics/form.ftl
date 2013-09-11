<h1>Internal statistics</h1>

<div class="alert alert-warning">
	<h3>Slow operations</h3>
	<p>
	Some of these operations (such as Hibernate statistics collection) is bad for performance and shouldn't
	be enabled in production, or at least not for any length of time.
	</p>
</div>

<div class="well">
<h2>Hibernate SessionFactory statistics</h2>

<p>
	These operations are sent asynchronously to each WAR via the message queue,
	so you may not see any effect except in the server log.
</p>

<button id="enable-hib-stats">Enable</button>

<button id="disable-hib-stats">Disable</button>

<button id="log-hib-stats">Output to log</button>

<button id="clear-hib-stats">Clear stats</button>

<div class="alert" id="hib-response"></div>

</div>

<script>
jQuery(function($){

	var debugResponse = function(data) {
		$('#hib-response').html(data);
	}

	$('#enable-hib-stats').click(function(e){
		e.preventDefault();
		$.post('/sysadmin/statistics/hibernate/toggleAsync.json', { enabled: true }, debugResponse);
	});

	$('#disable-hib-stats').click(function(e){
		e.preventDefault();
		$.post('/sysadmin/statistics/hibernate/toggleAsync.json', { enabled: false }, debugResponse);
	});

	$('#log-hib-stats').click(function(e){
		e.preventDefault();
		$.post('/sysadmin/statistics/hibernate/log', debugResponse);
	});

	$('#clear-hib-stats').click(function(e){
		e.preventDefault();
		$.post('/sysadmin/statistics/hibernate/clearAsync.json', debugResponse);
	});

})
</script>