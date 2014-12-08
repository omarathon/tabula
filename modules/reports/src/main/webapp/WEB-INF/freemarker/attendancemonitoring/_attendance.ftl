<#escape x as x?html>

<style>
	th.rotated .rotate {
		float: left;
		position: absolute;
		-webkit-transform-origin: 0% 100%;
		-moz-transform-origin: 0% 100%;
		-ms-transform-origin: 0% 100%;
		-o-transform-origin: 0% 100%;
		transform-origin: 0% 100%;
		-webkit-transform: rotateZ(90deg);
		-moz-transform: rotateZ(90deg);
		-ms-transform: rotateZ(90deg);
		-o-transform: rotateZ(90deg);
		transform: rotateZ(90deg);
		white-space: nowrap;
	}
	th.point {
		font-size: 80%;
		position: relative;
	}
	.tabula-page #main-content table.tablesorter > thead > tr th.header,
	.tabula-page #sb-container table.tablesorter > thead > tr th.header {
		background-position: bottom 10px right 0;
	}
</style>

<div><div class="sb-wide-table-wrapper">
	<table class="table table-bordered table-condensed table-striped table-sortable" style="width: auto; max-width: none;">
		<thead>
			<tr>
				<th class="sortable">First name</th>
				<th class="sortable">Last name</th>
				<th class="sortable">University ID</th>
				<#list points as point>
					<th class="point rotated">
						<div class="rotate">
							${point.name} (<@fmt.interval point.startDate point.endDate />)
						</div>
					</th>
				</#list>
				<th class="sortable"><i title="Unrecorded" class="icon-warning-sign icon-fixed-width late"></i></th>
				<th class="sortable"><i title="Missed monitoring points" class="icon-remove icon-fixed-width unauthorised"></i></th>
			</tr>
		</thead>
		<tbody>
			<#list students as student>
				<#assign missedCount = 0 />
				<#assign unrecordedCount = 0 />
				<tr>
					<td>
						${student.firstName}
					</td>
					<td>
						${student.lastName}
					</td>
					<td>
						<a href="<@routes.profile student />" target="_blank">${student.universityId}</a>
					</td>
					<#list points as point>
						<td>
							<#assign hasState = mapGet(mapGet(result, student), point)?? />
							<#if hasState>
								<#assign state = mapGet(mapGet(result, student), point).dbValue />
								<#if state == 'attended'>
									<i class="icon-fixed-width icon-ok attended"></i>
								<#elseif state == 'authorised'>
									<i class="icon-fixed-width icon-remove-circle authorised"></i>
								<#elseif state == 'unauthorised'>
									<i class="icon-fixed-width icon-remove unauthorised"></i>
									<#assign missedCount = missedCount + 1 />
								<#elseif point.late>
										<i class="icon-fixed-width icon-warning-sign late"></i>
										<#assign unrecordedCount = unrecordedCount + 1 />
								<#else>
									<i class="icon-fixed-width icon-minus unrecorded"></i>
								</#if>
							<#else>
								<i class="icon-fixed-width"></i>
							</#if>
						</td>
					</#list>
					<td class="unrecorded">
						<span class="badge badge-<#if (unrecordedCount > 2)>important<#elseif (unrecordedCount > 0)>warning<#else>success</#if>">
							${unrecordedCount}
						</span>
					</td>
					<td class="missed">
						<span class="badge badge-<#if (missedCount > 2)>important<#elseif (missedCount > 0)>warning<#else>success</#if>">
							${missedCount}
						</span>
					</td>
				</tr>
			</#list>
		</tbody>
	</table>
</div></div>

<script>
	jQuery(function($){
		$('th.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width + 20).css('width', height + 5);
			$(this).find('.rotate').css('margin-top', -(width + 25));
		});

		var popoutLinkHandler = function(event) {
			event.stopPropagation();
			event.preventDefault();
			if (!Shadowbox.initialized) {
				Shadowbox.initialized = true;
				Shadowbox.init(shadowboxOptions);
			}
			var tableWrapper = $(this).closest('div').find('div.sb-wide-table-wrapper');
			Shadowbox.open({
				link : this,
				content: '<div class="sb-wide-table-wrapper" style="background: white;">'
				+ tableWrapper.html()
				+ '</div>',
				player: 'html',
				width: $(window).width(),
				height: $(window).height(),
				options: {
					onFinish: function(){
						$('#sb-container').find('.table-sortable')
							.find('th.header').removeClass('header')
							.end().removeClass('tablesorter').sortableTable();
					}
				}
			});
		};

		var generatePopoutLink = function(){
			return $('<span/>')
					.addClass('sb-table-wrapper-popout')
					.append('(')
					.append(
					$('<a/>')
							.attr('href', '#')
							.html('Pop-out table')
							.on('click', popoutLinkHandler)
			).append(')');
		};

		$('div.sb-wide-table-wrapper > table').each(function(){
			var $this = $(this);
			$this.parent().parent('div').prepend(generatePopoutLink()).append(generatePopoutLink())
		}).sortableTable();
	})
</script>

</#escape>