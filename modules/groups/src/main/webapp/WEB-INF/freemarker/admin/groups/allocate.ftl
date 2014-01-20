<#import "../../group_components.ftl" as components />

<#assign set=allocateStudentsToGroupsCommand.set />
<#assign mappingById=allocateStudentsToGroupsCommand.mappingById />
<#assign membersById=allocateStudentsToGroupsCommand.membersById />



<#macro student_item student bindpath="">
	<#local profile = membersById[student.warwickId]!{} />
	<li class="student well well-small"
	data-f-gender="${(profile.gender.dbValue)!}"
	data-f-route="${(profile.mostSignificantCourseDetails.route.code)!}"
	data-f-year="${(profile.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}">
		<div class="profile clearfix">
			<@fmt.member_photo profile "tinythumbnail" false />
			<div class="name">
				<h6>${profile.fullName!student.fullName}</h6>
				${(profile.mostSignificantCourseDetails.route.name)!student.shortDepartment!""}
			</div>
		</div>
		<input type="hidden" name="${bindpath}" value="${student.userId}" />
	</li>
</#macro>

<#escape x as x?html>
	<h1>Allocate students to ${set.name}</h1>
	<#if (allocateStudentsToGroupsCommand.isStudentSignup())>
		<div class="alert">These groups are currently <strong>${set.openForSignups?string("open","closed")}</strong> for self sign-up</div>
	</#if>
	<noscript>
		<div class="alert">This page requires Javascript.</div>
	</noscript>

	<div class="tabbable">
		<ul class="nav nav-tabs">
			<li class="active">
				<a href="#allocategroups-tab1" data-toggle="tab">Drag and drop</a>
			</li>
			<li >
				<a href="#allocategroups-tab2" data-toggle="tab">Upload spreadsheet</a>
			</li>
		</ul>

		<div class="tab-content">
			<div class="tab-pane active" id="allocategroups-tab1">

			<p>Drag students onto a group to allocate them to it. Select multiple students by dragging a box around them.
				 You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key to add to a selection.</p>

			<@spring.hasBindErrors name="allocateStudentsToGroupsCommand">
				<#if errors.hasErrors()>
					<div class="alert alert-error">
						<h3>Some problems need fixing</h3>
						<#if errors.hasGlobalErrors()>
							<#list errors.globalErrors as e>
								<div><@spring.message message=e /></div>
							</#list>
						<#elseif errors.hasFieldErrors('file')>
							<#list errors.getFieldErrors('file') as e>
								<div><@spring.message message=e /></div>
							</#list>
						<#else>
							<div>See the errors below.</div>
						</#if>
					</div>
				</#if>
			</@spring.hasBindErrors>

			<#assign submitUrl><@routes.allocateset set /></#assign>
			<div class="persist-area">
				<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">
				<div class="tabula-dnd"
						 data-item-name="student"
						 data-text-selector=".name h6"
						 data-use-handle="false"
						 data-selectables=".students .drag-target"
						 data-scroll="true"
						 data-remove-tooltip="Remove this student from this group">
					<div class="persist-header">
						<div class="btn-toolbar">
							<a class="random btn" data-toggle="randomise" data-disabled-on="empty-list"
							   href="#" >
								<i class="icon-random"></i> Randomly allocate
							</a>
							<a class="return-items btn" data-toggle="return" data-disabled-on="no-allocation"
							   href="#" >
								<i class="icon-arrow-left"></i> Remove all
							</a>
						</div>
						<div class="row-fluid hide-smallscreen">
							<div class="span5">
								<h3>Students</h3>
							</div>
							<div class="span2"></div>
							<div class="span5">
								<h3>Groups</h3>
							</div>
						</div>
					</div><!-- end persist header -->

					<div class="row-fluid fix-on-scroll-container">
						<div class="span5">
							<div id="studentslist"
								 class="students tabula-filtered-list"
								 data-item-selector=".student-list li">
								<div class="well ">
									<h4>Not allocated to a group</h4>
									<#if features.smallGroupAllocationFiltering>
										<div class="filter" id="filter-by-gender-controls">
											<select data-filter-attr="fGender">
												<option data-filter-value="*">Any Gender</option>
												<option data-filter-value="M">Male</option>
												<option data-filter-value="F">Female</option>
											</select>
										</div>
										<div class="filter" id="filter-by-year-controls">
											<select data-filter-attr="fYear">
												<option data-filter-value="*">Any Year of study</option>
												<#list allocateStudentsToGroupsCommand.allMembersYears as year>
													<option data-filter-value="${year}">Year ${year}</option>
												</#list>
											</select>
										</div>
										<div class="filter" id="filter-by-route-controls">
											<select data-filter-attr="fRoute">
												<option data-filter-value="*">Any Route</option>
												<#list allocateStudentsToGroupsCommand.allMembersRoutes as route>
													<option data-filter-value="${route.code}"><@fmt.route_name route /></option>
												</#list>
											</select>
										</div>
									</#if>
									<div class="student-list drag-target">
										<ul class="drag-list return-list unstyled" data-bindpath="unallocated">
											<@spring.bind path="unallocated">
											<#list status.actualValue as student>
												<@student_item student "${status.expression}[${student_index}]" />
											</#list>
										</@spring.bind>
										</ul>
									</div>
								</div>
							</div>
						</div>
						<div class="span2">
							<#-- I, for one, welcome our new jumbo icon overlords -->
							<div class="direction-icon fix-on-scroll hide-smallscreen">
								<i class="icon-arrow-right"></i>
							</div>
						</div>
						<div class="span5">
							<h3 class="smallscreen-only">Groups</h3>
							<div id="groupslist" class="groups fix-on-scroll">
								<#list set.groups as group>
								<#assign existingStudents = mappingById[group.id]![] />
								<div class="drag-target well clearfix group-${group.id}">
									<div class="group-header">
										<#assign popoverHeader>Students in ${group.name}</#assign>
										<#assign groupDetails>
											<ul class="unstyled">
												<#list group.events as event>
													<li>
														<@components.event_schedule_info event />
													</li>
												</#list>
											</ul>
										</#assign>

										<h4 class="name">
											${group.name}
										</h4>

										<div>
											<#assign count = existingStudents?size />
											<span class="drag-count">${count}</span> <span class="drag-counted" data-singular="student" data-plural="students">student<#if count != 1>s</#if></span>

											<a id="show-list-${group.id}" class="show-list" title="View students" data-container=".group-${group.id}" data-title="${popoverHeader}" data-prelude="${groupDetails}" data-placement="left"><i class="icon-question-sign"></i></a>
										</div>
									</div>

									<ul class="drag-list hide" data-bindpath="mapping[${group.id}]">
										<#list existingStudents as student>
											<@student_item student "mapping[${group.id}][${student_index}]" />
										</#list>
									</ul>
								</div>
							</#list>
							</div>
						</div>

					</div>

				</div>

					<div class="submit-buttons persist-footer">
						<input type="submit" class="btn btn-primary" value="Save">
						<a href="<@routes.depthome module />" class="btn">Cancel</a>
					</div>
				</@f.form>
			</div>
			</div><!-- end 1st tab -->

			<div class="tab-pane" id="allocategroups-tab2">

				<@f.form method="post" enctype="multipart/form-data" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">

					<p>You can allocate students to groups using a spreadsheet.</p>

					<ol>
						<li><strong><a href="allocate/template">Download a template spreadsheet</a></strong>. This will be prefilled with the names and University ID numbers of students you have selected to be in ${set.name}. In Excel you may need to <a href="http://office.microsoft.com/en-gb/excel-help/what-is-protected-view-RZ101665538.aspx?CTT=1&section=7">exit protected view</a> to edit the spreadsheet.
						</li>
						<li><strong>Allocate students</strong> to groups using the dropdown menu in the <strong>Group name</strong> column or by pasting in a list of group names. The group names must match the groups you have already created for ${set.name}. The <strong>group_id</strong> field will be updated with a unique ID number for that group.
							You can select additional students to be in ${set.name} by entering their University ID numbers in the <strong>student_id</strong> column. Any students with an empty group_id field will be added to the list of students who haven't been allocated to a group.</li>
						<li><strong>Save</strong> your updated spreadsheet.</li>
					    <li><@form.labelled_row "file.upload" "Choose your updated spreadsheet" "step-action" ><input type="file" name="file.upload"  /> </@form.labelled_row></li>
					</ol>

					<input name="isfile" value="true" type="hidden"/>


					<div class="submit-buttons">
						<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
					</div>
				</@f.form>

			</div><!-- end 2nd tab-->

		</div><!-- end tab-content -->

	</div> <!-- end tabbable -->

	<script type="text/javascript">
		(function($) {
			<!--TAB-1008 - fix scrolling bug when student list is shorter than the group list-->
			$('#studentslist').css('min-height', function() {
				return $('#groupslist').outerHeight();
			});

			var fixHeaderFooter = $('.persist-area').fixHeaderFooter();

			$(window).scroll(function() {
				fixHeaderFooter.fixDirectionIcon();
				fixHeaderFooter.fixTargetList('#groupslist'); // eg. personal tutors column
			});

			// When the return list has changed, make sure the filter is re-run
			$('.return-list').on('changed.tabula', function(e) {
				// Make sure it exists before doing it
				var filter = $('.tabula-filtered-list').data('tabula-filtered-list');
				if (filter) {
					filter.filter();
				}
			});
		})(jQuery);
	</script>

</#escape>