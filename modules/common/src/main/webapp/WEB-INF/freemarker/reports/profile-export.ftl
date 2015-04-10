<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		body { font-family: "Helvetica Neue", Helvetica, Arial, sans-serif; }
	</style>
</head>
<body>
	<#macro pointInATerm points state term>
		<div class="term">
			<h2>${state} Monitoring Points, Term: ${term}</h2>

			<#list points?sort_by("startDate") as point>
				<h3>Monitoring Point Name: ${point.name}</h3>
				<table>
					<thead>
						<tr>
							<th>Criteria</th>
							<th>Details</th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td>Monitoring Point Type</td>
							<td>${point.pointType}</td>
						</tr>
						<tr>
							<td>Monitoring Point Type Information</td>
							<td>${point.pointTypeInfo}</td>
						</tr>
						<tr>
							<td>Start Date</td>
							<td>${point.startDate}</td>
						</tr>
						<tr>
							<td>End Date</td>
							<td>${point.endDate}</td>
						</tr>
						<tr>
							<td>Recorded By</td>
							<td>${point.recordedBy.fullName!""} (${point.recordedBy.userId!"unknown"})</td>
						</tr>
						<tr>
							<td>Last Recorded Date</td>
							<td>${point.recordedDate}</td>
						</tr>
						<#if point.attendanceNote??>
							<tr>
								<td>Attendance Note</td>
								<td>
									<p>Reason: ${point.attendanceNote.absenceType.description}</p>
									Comments: <br/>
									${point.attendanceNote.escapedNote}
								</td>
							</tr>
							<tr>
								<td>Attendance Note Attachment</td>
								<td>
									<#if point.attendanceNote.attachment?has_content>
										${point.attendanceNote.attachment.id}-${point.attendanceNote.attachment.name}
									<#else>
										None
									</#if>
								</td>
							</tr>
						<#else>
							<tr>
								<td>Attendance Note</td>
								<td>None</td>
							</tr>
							<tr>
								<td>Attendance Note Attachments</td>
								<td>None</td>
							</tr>
						</#if>

					</tbody>
				</table>
			</#list>
		</div>
	</#macro>
	<#assign terms = ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] />
	<#list groupedPoints?keys?sort as department>
		<div class="department">
			<h1>Monitoring points for ${department}</h1>
			<div class="state">
				<#if groupedPoints[department]["attended"]??>
					<#list terms as term>
						<#if groupedPoints[department]["attended"][term]??>
							<@pointInATerm groupedPoints[department]["attended"][term] "Attended" term />
						</#if>
					</#list>
				</#if>
				<#if groupedPoints[department]["unauthorised"]??>
					<#list terms as term>
						<#if groupedPoints[department]["unauthorised"][term]??>
							<@pointInATerm groupedPoints[department]["unauthorised"][term] "Missed" term />
						</#if>
					</#list>
				</#if>
				<#if groupedPoints[department]["authorised"]??>
					<#list terms as term>
						<#if groupedPoints[department]["authorised"][term]??>
							<@pointInATerm groupedPoints[department]["authorised"][term] "Authorised Absence" term />
						</#if>
					</#list>
				</#if>
			</div>
		</div>
	</#list>
</body>
</html>
</#escape>