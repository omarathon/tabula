<#escape x as x?html>

<style>
	th.rotated .rotate, td.rotated .rotate {
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
	th.module {
		font-size: 80%;
		position: relative;
	}
	.tabula-page #main-content table.tablesorter > thead > tr th.header,
	.tabula-page #sb-container table.tablesorter > thead > tr th.header {
		background-position: bottom 10px right 0;
	}
	.tabula-page #main-content table.tablesorter > thead > tr th.header.rotated,
	.tabula-page #sb-container table.tablesorter > thead > tr th.header.rotated {
		background-position: bottom 10px center;
	}
</style>

<div><div class="sb-wide-table-wrapper">
	<table class="table table-bordered table-condensed table-striped table-sortable" style="width: auto; max-width: none;">
		<thead>
			<tr>
				<th class="sortable">First name</th>
				<th class="sortable">Last name</th>
				<th class="sortable">University ID</th>
				<#list modules as module>
					<th class="module rotated sortable">
						<div class="rotate">
							${module.code?upper_case}
							${module.name}
						</div>
					</th>
				</#list>
			</tr>
		</thead>
		<tbody>
			<#list students as student>
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
					<#list modules as module>
						<td>
							<#assign hasCount = mapGet(mapGet(counts, student), module)?? />
							<#if hasCount>
								<#assign count = mapGet(mapGet(counts, student), module) />
								<span class="badge badge-<#if (count > 2)>important<#elseif (count > 0)>warning<#else>success</#if>">
									${count}
								</span>
							<#else>
								<i class="icon-fixed-width"></i>
							</#if>
						</td>
					</#list>
				</tr>
			</#list>
		</tbody>
	</table>
</div></div>

<script>
	jQuery(function($){
		$('th.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			$(this).css('height', width + 35);
			$(this).find('.rotate').css('margin-top', -(width + 40));
		});
		$('td.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width).css('width', height + 5);
			$(this).find('.rotate').css('margin-top', -(height));
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