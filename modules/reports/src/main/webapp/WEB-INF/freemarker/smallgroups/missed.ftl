<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<h1>Missed event attendance</h1>

<#assign reportUrl><@routes.missedSmallGroups department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl>
	<div class="btn-toolbar">
		<a href="#" class="show-data btn" data-loading-text="Loading&hellip;" data-href="<@routes.missedSmallGroupsShow department academicYear />">
			<i class="icon-eye-open"></i> Show
		</a>
		<div class="download btn-group ">
			<a href="#" class="btn dropdown-toggle" data-toggle="dropdown">
				<i class="icon-download"></i> Download&hellip;
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">

				<li>
					<a href="#" data-href="<@routes.missedSmallGroupsDownloadCsv department academicYear />">
						<i class="icon-table"></i> CSV
					</a>
				</li>
				<li>
					<a href="#" data-href="<@routes.missedSmallGroupsDownloadXlsx department academicYear />">
						<i class="icon-list-alt"></i> Excel
					</a>
				</li>
				<li>
					<a href="#" data-href="<@routes.missedSmallGroupsDownloadXml department academicYear />">
						<i class="icon-code"></i> XML
					</a>
				</li>
			</ul>
		</div>
	</div>
</@reports_macros.reportLoader>

</#escape>