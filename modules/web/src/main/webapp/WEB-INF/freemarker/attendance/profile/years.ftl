<h1>My Monitoring Points</h1>

<#if student??>
	<p><em>Choose academic year to view:</em></p>

	<ul class="links">
		<li>
			<#if years?seq_contains(2013)>
				<h3><a href="<@routes.attendance.profileForYear student '2013' />">13/14</a></h3>
			</#if>
			<#if years?seq_contains(2014) && features.academicYear2014>
				<h3><a href="<@routes.attendance.profileForYear student '2014' />">14/15</a></h3>
			</#if>
			<#if years?seq_contains(2015) && features.academicYear2015>
				<h3><a href="<@routes.attendance.profileForYear student '2015' />">15/16</a></h3>
			</#if>
			<#if years?seq_contains(2016) && features.academicYear2016>
				<h3><a href="<@routes.attendance.profileForYear student '2016' />">16/17</a></h3>
			</#if>
		</li>
	</ul>
<#else>
	<p><em>There are no monitoring points recorded for you for this academic year.</em></p>
</#if>