<#escape x as x?html>

	<#macro previousExtensions extensionId warwickId fullName acceptedExtensions rejectedExtensions previousExtensions>

		<#if previousExtensions?has_content>
			<div id="prev-extensions-${extensionId}" class="modal fade" role="dialog">
				<div class="modal-dialog" role="document">
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal" aria-label="Close">
								<span aria-hidden="true">&times;</span>
							</button>
							<h4 class="modal-title">Previous extension requests</h4>
						</div>
						<div class="modal-body">
							<h5>${fullName} - ${warwickId}</h5>
							<div><strong>Accepted requests: </strong> ${acceptedExtensions}</div>
							<div><strong>Denied requests: </strong> ${rejectedExtensions}</div>
							<table class="table table-striped">
								<thead>
								<tr>
									<th>Module</th>
									<th>Assignment</th>
									<th>Status</th>
									<th>Made</th>
								</tr>
								</thead>
								<tbody>
									<#list previousExtensions as e>
									<tr>
										<td>${e.assignment.module.code}</td>
										<td>${e.assignment.name}</td>
										<td>${e.state.description}</td>
										<td>
											<#if e.requestedOn?has_content>
													<@fmt.date date=e.requestedOn />
												<#else>
												<@fmt.date date=e.reviewedOn />
											</#if>
										</td>
									</tr>
									</#list>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</#if>
	</#macro>

	<#macro previousSubmissions extensionId warwickId fullName previousSubmissions>
		<#if previousSubmissions?has_content>
			<div id="prev-submissions-${extensionId}" class="modal fade" role="dialog">
				<div class="modal-dialog" role="document">
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal" aria-label="Close">
								<span aria-hidden="true">&times;</span>
							</button>
							<h4 class="modal-title">Previous submissions</h4>
						</div>
						<div class="modal-body">
							<h5>${fullName} - ${warwickId}</h5>
							<table class="table table-striped">
								<thead>
								<tr>
									<th>Module</th>
									<th>Assignment</th>
									<th>Status</th>
								</tr>
								</thead>
								<tbody>
								<#list previousSubmissions as submission>
								<tr>
									<td>${submission.assignment.module.code}</td>
									<td>${submission.assignment.name}</td>
									<td>
									<#if submission.isAuthorisedLate()>
										Within extension
									<#elseif submission.isLate()>
										Late
									<#else>
										On time
									</#if>
									</td>
								</tr>
								</#list>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</#if>
	</#macro>

	<#macro coursework_sits_groups command >

		<#if command.availableUpstreamGroups?has_content>
			<div class="assessment-component assignment-modal">
				<table id="sits-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
					<thead>
					<tr>
						<th class="for-check-all"><input  type="checkbox" class="check-all use-tooltip" title="Select all/none" /> </th>
						<th class="sortable">Name</th>
						<th class="sortable">Members</th>
						<th class="sortable">Assessment group</th>
						<th class="sortable">CATS</th>
						<th class="sortable">Occurrence</th>
						<th class="sortable">Sequence</th>
						<th class="sortable">Type</th>
					</tr>
					</thead>
					<tbody>
						<#list command.availableUpstreamGroups as available>
							<#local isLinked = available.isLinked(command.assessmentGroups) />
						<tr>
							<td>
								<input
										type="checkbox"
										id="chk-${available.id}"
										value="${available.id}"
								${isLinked?string(" checked","")}
							</td>
							<td><label for="chk-${available.id}">${available.name}<span class="label label-primary linked <#if !isLinked>hidden</#if>">Linked</span></label></td>
							<td class = "sortable">${available.group.members?size}</td>
							<td>${available.group.assessmentGroup}</td>
							<td>${available.cats!'-'}</td>
							<td>${available.occurrence}</td>
							<td>${available.sequence}</td>
							<td>${available.assessmentType!'A'}</td>
						</tr>
						</#list>
					</tbody>
				</table>
				<div class="sits-picker">
					<a class="btn btn-primary disabled sits-picker sits-picker-action spinnable spinner-auto link-sits" data-url="<@routes.cm2.enrolment command.assignment />">Link</a>
					<a class="btn btn-danger disabled sits-picker sits-picker-action spinnable spinner-auto unlink-sits" data-url="<@routes.cm2.enrolment command.assignment/>">Unlink</a>
				</div>
			</div>

		<#else>
		<div class="modal-body">
			<p class="alert alert-danger">No SITS membership groups for ${command.module.code?upper_case} are available</p>
		</div>
		</#if>
	</#macro>

</#escape>