<#escape x as x?html>
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#macro deptheaderroutemacro dept><@routes.relationship_allocate dept relationshipType /></#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<#assign manualFormAction><@routes.relationship_allocate department relationshipType /></#assign>
<#assign uploadFormAction><@routes.relationship_allocate_upload department relationshipType /></#assign>
<#assign previewFormAction><@routes.relationship_allocate_preview department relationshipType /></#assign>

<@fmt.deptheader "Allocate ${relationshipType.description}s" "in" department routes "deptheaderroute" "with-settings" />

<div class="tabbable">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#allocatestudents-tab1" data-toggle="tab">Manually allocate students</a>
		</li>
		<li>
			<a href="#allocatestudents-tab2" data-toggle="tab">Upload spreadsheet</a>
		</li>
	</ul>
</div>

<div class="tab-content">
	<div id="allocatestudents-tab1" class="tab-pane active fix-area allocate-associations">
		<#macro filter path placeholder currentFilter allItems validItems=allItems prefix="" customPicker="" cssClass="">
			<@spring.bind path=path>
			<div class="btn-group ${cssClass} <#if currentFilter == placeholder> empty-filter</#if>">
				<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown">
					<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
					<span class="caret"></span>
				</a>
				<div class="dropdown-menu filter-list">
					<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
					<ul>
						<#if customPicker?has_content>
							<li>
								<#noescape>${customPicker}</#noescape>
							</li>
						</#if>
						<#if allItems?has_content>
							<#list allItems as item>
								<#local isValid = (allItems?size == validItems?size)!true />
								<#if !isValid>
									<#list validItems as validItem>
										<#if ((validItem.id)!0) == ((item.id)!0)>
											<#local isValid = true />
										</#if>
									</#list>
								</#if>
								<li class="check-list-item" data-natural-sort="${item_index}">
									<label class="checkbox <#if !isValid>disabled</#if>">
										<#nested item isValid/>
									</label>
								</li>
							</#list>
						<#else>
							<li><small class="muted" style="padding-left: 5px;">N/A for this department</small></li>
						</#if>
					</ul>
				</div>
			</div>
			</@spring.bind>
		</#macro>

		<#macro current_filter_value path placeholder><#compress>
			<@spring.bind path=path>
				<#if status.actualValue?has_content>
					<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
				<#else>
				${placeholder}
				</#if>
			</@spring.bind>
		</#compress></#macro>

		<#function contains_by_code collection item>
			<#list collection as c>
				<#if c.code == item.code>
					<#return true />
				</#if>
			</#list>
			<#return false />
		</#function>

		<@f.form commandName="command" action="${manualFormAction}" method="POST" cssClass="form-inline fetch">
			<#list command.additions?keys as entity>
				<#list command.additions[entity] as student>
					<input type="hidden" name="additions[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.removals?keys as entity>
				<#list command.removals[entity] as student>
					<input type="hidden" name="removals[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.additionalEntities as entity>
				<input type="hidden" name="additionalEntities" value="${entity}" />
			</#list>
			<div class="row-fluid">
				<div class="span6 students">
					<div class="header-with-tooltip">
						<h3>Students</h3>
						<span><@fmt.p unallocated?size "Unallocated student" /> found</span>
					</div>

					<div class="student-filter btn-group-group well well-small">
						<#assign placeholder = "All routes" />
						<#assign currentfilter><@current_filter_value "routes" placeholder; route>${route.code?upper_case}</@current_filter_value></#assign>
						<#assign routesCustomPicker>
							<div class="route-search input-append">
								<input class="route-search-query route prevent-reload" type="text" value="" placeholder="Search for a route" />
								<span class="add-on"><i class="icon-search"></i></span>
							</div>
						</#assign>
						<@filter path="routes" placeholder=placeholder currentFilter=currentfilter allItems=command.allRoutes validItems=command.visibleRoutes customPicker=routesCustomPicker cssClass="wide"; route, isValid>
							<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}" ${contains_by_code(command.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
							<@fmt.route_name route false />
						</@filter>

						<#assign placeholder = "All years" />
						<#assign currentfilter><@current_filter_value "yearsOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
						<@filter "yearsOfStudy" placeholder currentfilter command.allYearsOfStudy command.allYearsOfStudy "Year " "" "narrow"; yearOfStudy>
							<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
							${command.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
						${yearOfStudy}
						</@filter>

						<button class="btn btn-mini apply" type="submit">Apply</button>

						<br /><br />

						<div class="input-append">
							<input class="input-xlarge" name="query" type="text" placeholder="Search these students" value="${command.query!}"/>
							<button class="btn" type="submit"><i class="icon-search"></i></button>
						</div>
					</div>

					<#assign singleUnallocated = unallocated?has_content && unallocated?size == 1 />
					<#if unallocated?has_content>

						<button class="btn distribute" name="action" value="${commandActions.Distribute}" type="submit">Distribute between selected personal tutors <i class="icon-arrow-right"></i></button>

						<br /> <br />

						<table class="table table-condensed table-bordered table-striped table-hover scrollable-tbody">
							<thead>
								<tr>
									<th class="check for-check-all"></th>
									<th class="single-name sortable">First name</th>
									<th class="single-name sortable">Last name</th>
									<th class="universityid sortable">ID</th>
								</tr>
							</thead>
							<tbody>
								<#list unallocated as studentData>
									<tr>
										<td class="check"><input type="checkbox" name="allocate" value="${studentData.universityId}" <#if singleUnallocated>checked</#if>></td>
										<td class="single-name">${studentData.firstName}</td>
										<td class="single-name">${studentData.lastName}</td>
										<td class="universityid">${studentData.universityId} <@pl.profile_link studentData.universityId /></td>
									</tr>
								</#list>
							</tbody>
						</table>

					</#if>

				</div>

				<div class="span6 entities">
					<div class="header-with-tooltip">
						<h3>${relationshipType.description}s</h3>
						<span><@fmt.p allocated?size "${relationshipType.description}" /> found</span>
					</div>

					<div class="student-filter btn-group-group well well-small">
						<#assign placeholder = "All ${relationshipType.description}s" />
						<#assign currentfilter><@current_filter_value "entityTypes" placeholder; entityType>${command.allEntityTypesLabels[entityType]}</@current_filter_value></#assign>
						<@filter path="entityTypes" placeholder=placeholder currentFilter=currentfilter allItems=command.allEntityTypes validItems=command.allEntityTypes cssClass="wide"; entityType, isValid>
							<input type="checkbox" name="${status.expression}" value="${entityType}" data-short-value="${command.allEntityTypesLabels[entityType]}"
								${command.entityTypes?seq_contains(entityType)?string('checked','')}
							>
							${command.allEntityTypesLabels[entityType]}
						</@filter>

						<button class="btn btn-mini apply" type="submit">Apply</button>

						<br /><br />

						<button type="button" class="btn" data-toggle="modal" data-target="#add-agents">
							Add ${relationshipType.agentRole}s</button>
					</div>

					<button class="btn remove-all" name="action" value="${commandActions.RemoveFromAll}" type="submit" title="You need to select some personal tutors from which to remove students">
						<i class="icon-arrow-left"></i> Remove all students from selected ${relationshipType.description}(s)
					</button>

					<br /> <br />

					<table class="table table-condensed table-bordered table-striped table-hover scrollable-tbody">
						<thead >
						<tr>
							<th class="check for-check-all"></th>
							<th class="full-name sortable">${relationshipType.description}s name</th>
							<th class="counter sortable">Students</th>
							<th class="edit"></th>
						</tr>
						</thead>
						<tbody>
							<#list allocated?sort_by("sortName") as entityData>
								<tr data-entity="${entityData.entityId}" <#if command.expanded[entityData.entityId]!false>class="expanded"</#if>>
									<td class="check"><input type="checkbox" name="entities" value="${entityData.entityId}" /></td>
									<td class="full-name" data-sortby="${entityData.sortName}">${entityData.displayName}</td>
									<td class="counter">${entityData.students?size}</td>
									<td class="toggle">
										<i title="Edit students allocated to this ${relationshipType.agentRole}" class="icon-edit icon-large icon-fixed-width <#if !entityData.students?has_content>icon-muted</#if>"></i>
										<input type="hidden" name="expanded[${entityData.entityId}]" value="${(command.expanded[entityData.entityId]!false)?string}" />
									</td>
								</tr>
								<#list entityData.students?sort_by("lastName", "firstName") as studentData>
									<tr data-forentity="${entityData.entityId}" class="forentity <#if !studentData_has_next>last</#if>">
										<td class="student" colspan="3">${studentData.firstName} ${studentData.lastName} (${studentData.universityId})  <@pl.profile_link studentData.universityId /></td>
										<td class="remove">
											<button title="Remove" class="btn btn-link" type="submit" name="removeSingleCombined" value="removeSingle-${entityData.entityId}-${studentData.universityId}"><i class="icon-remove icon-large icon-fixed-width"></i></button>
										</td>
									</tr>
								</#list>
							</#list>
						</tbody>
					</table>

				</div>
			</div>

			<div class="modal fade hide" id="add-agents" tabindex="-1" role="dialog" aria-labelledby="add-agents-label" aria-hidden="true">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h3 id="add-agents-label">Add ${relationshipType.agentRole}s</h3>
				</div>

				<div class="modal-body">
					<p>
						Lookup ${relationshipType.agentRole}s by typing their names, usercodes or university IDs below, then click <code>Add</code>.
					</p>

					<@form.labelled_row "additionalEntityUserIds" "${relationshipType.agentRole?cap_first}s">
						<@form.flexipicker path="additionalEntityUserIds" placeholder="User name" membersOnly="true" list=true multiple=true />
					</@form.labelled_row>
				</div>

				<div class="modal-footer">
					<button type="submit" class="btn btn-primary" name="action" value="${commandActions.AddAdditionalEntities}">Add</button>
				</div>
			</div>
		</@f.form>

		<@f.form commandName="command" action="${previewFormAction}" method="POST" cssClass="form-inline preview">
			<#list command.additions?keys as entity>
				<#list command.additions[entity] as student>
				<input type="hidden" name="additions[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.removals?keys as entity>
				<#list command.removals[entity] as student>
				<input type="hidden" name="removals[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.additionalEntities as entity>
				<input type="hidden" name="additionalEntities" value="${entity}" />
			</#list>
			<input type="hidden" name="allocationType" value="${allocationTypes.Add}" />

			<div class="submit-buttons fix-footer">
				<button type="submit" class="btn btn-primary">Save</button>
				<a href="<@routes.home />" class="btn">Cancel</a>
			</div>
		</@f.form>
	</div>

	<div class="tab-pane" id="allocatestudents-tab2">
		<@f.form commandName="uploadCommand" action="${uploadFormAction}" method="POST" cssClass="form-inline" enctype="multipart/form-data">
			<#list command.additions?keys as entity>
				<#list command.additions[entity] as student>
					<input type="hidden" name="additions[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.removals?keys as entity>
				<#list command.removals[entity] as student>
					<input type="hidden" name="removals[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.additionalEntities as entity>
				<input type="hidden" name="additionalEntities" value="${entity}" />
			</#list>

			<#assign introText>
				<p>The spreadsheet must be in <samp>.xlsx</samp> format (created in Microsoft Excel 2007 or newer, or another compatible spreadsheet application). You can download a template spreadsheet which is correctly formatted, ready for completion.<p>
				<p>The spreadsheet must contain two columns, headed:<p>
				<ul>
					<li><b>student_id</b> - contains the student's University ID number (also known as the library card number)</li>
					<li><b>agent_id</b> - contains the ${relationshipType.agentRole}'s University ID number</li>
				</ul>
				<p>You may need to <a href='http://office.microsoft.com/en-gb/excel-help/format-numbers-as-text-HA102749016.aspx?CTT=1'>format these columns</a> as text to avoid Microsoft Excel removing 0s from the start of ID numbers.</p>
				<p>The spreadsheet may also contain other columns and information for your own reference (these will be ignored by Tabula).</p>
			</#assign>

			<p>You can set ${relationshipType.agentRole}s for many students at once by uploading a spreadsheet.
				<a href="#"
				   id="agent-intro"
				   class="use-introductory"
				   data-hash="${introHash("agent-intro")}"
				   data-title="${relationshipType.agentRole} spreadsheet"
				   data-trigger="click"
				   data-placement="bottom"
				   data-html="true"
				   data-content="${introText}"><i class="icon-question-sign"></i></a></p>

			<ol>
				<li>
					<button type="submit" class="btn" name="templateWithChanges" value="true"><i class="icon-download"></i> Download a template spreadsheet</button>
					<br />
					This will be prefilled with the names and University ID numbers of students and their ${relationshipType.agentRole} (if they have one) in ${department.name}. In Excel you may need to <a href="http://office.microsoft.com/en-gb/excel-help/what-is-protected-view-RZ101665538.aspx?CTT=1&section=7">exit protected view</a> to edit the spreadsheet.
					<br /><br />
					<div class="alert alert-info">
						<p>This will include any changes made in the drag and drop tab. You can also <a href="<@routes.relationship_template department relationshipType />">download a template without these changes</a>.</p>
					</div>
				</li>
				<li><strong>Allocate students</strong> to ${relationshipType.agentRole}s using the dropdown menu in the <strong>${relationshipType.agentRole?cap_first} name</strong> column or by typing a ${relationshipType.agentRole}'s University ID into the <strong>agent_id</strong> column. The <strong>agent_id</strong> field will be updated with the University ID for that ${relationshipType.agentRole} if you use the dropdown.</li>
				<li><strong>Save</strong> your updated spreadsheet.</li>
				<li><@form.labelled_row "file.upload" "Choose your updated spreadsheet" "step-action" ><input type="file" name="file.upload"  /> </@form.labelled_row></li>
			</ol>


			<div class="submit-buttons">
				<button class="btn btn-primary btn-large" name="doPreviewSpreadsheetUpload"><i class="icon-upload icon-white"></i> Upload</button>
			</div>
		</@f.form>
	</div>
</div>

<div id="profile-modal" class="modal fade profile-subset"></div>

</#escape>
