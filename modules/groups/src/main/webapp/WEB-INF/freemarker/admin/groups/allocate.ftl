<#assign set=allocateStudentsToGroupsCommand.set />
<#assign mappingById=allocateStudentsToGroupsCommand.mappingById />
<#assign membersById=allocateStudentsToGroupsCommand.membersById />



<#macro student_item student bindpath="">
	<#assign profile = membersById[student.warwickId]!{} />
	<li class="student well well-small">
		<div class="profile clearfix">
			<@fmt.member_photo profile "tinythumbnail" false />

			<div class="name">
				<h6>${profile.fullName!student.fullName}</h6>
				${(profile.mostSignificantCourseDetails.route.name)!student.shortDepartment}
			</div>
		</div>
		<input type="hidden" name="${bindpath}" value="${student.userId}" />
	</li>
</#macro>

<#escape x as x?html>
	<h1>Allocate students to ${set.name}</h1>

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
						<#else>
							<div>See the errors below.</div>
						</#if>
					</div>
				</#if>
			</@spring.hasBindErrors>

			<#assign submitUrl><@routes.allocateset set /></#assign>
			<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">
				<div class="btn-toolbar">
					<a class="random btn"
					   href="#" >
						<i class="icon-random"></i> Randomly allocate
					</a>
					<a class="return-items btn"
					   href="#" >
						<i class="icon-arrow-left"></i> Remove all
					</a>
				</div>
				<div class="row-fluid fix-on-scroll-container">
					<div class="span5">
						<div class="students">
							<h3>Students</h3>
							<div class="well student-list drag-target">
								<h4>Not allocated to a group</h4>

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
					<div class="span2">
						<#-- I, for one, welcome our new jumbo icon overlords -->
						<div class="direction-icon fix-on-scroll">
							<i class="icon-arrow-right"></i>
						</div>
					</div>
					<div class="span5">
						<div class="groups fix-on-scroll">
							<h3>Groups</h3>
							<#list set.groups as group>
								<#assign existingStudents = mappingById[group.id]![] />
								<div class="drag-target well clearfix">
									<div class="group-header">
										<#assign popoverHeader>Students in ${group.name}</#assign>
										<#assign groupDetails>
											<ul class="unstyled">
												<#list group.events as event>
													<li>
														<#-- Tutor, weeks, day/time, location -->

														<@fmt.weekRanges event />,
														${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime />,
														${event.location!"[no location]"}
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

											<a id="show-list-${group.id}" class="show-list" title="View students" data-container=".groups .drag-target" data-title="${popoverHeader}" data-prelude="${groupDetails}" data-placement="left"><i class="icon-question-sign"></i></a>
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

				<div class="submit-buttons">
					<input type="submit" class="btn btn-primary" value="Save">
					<a href="<@routes.depthome module />" class="btn">Cancel</a>
				</div>
			</@f.form>
			</div><!-- end 1st tab -->

			<div class="tab-pane" id="allocategroups-tab2">

				<@f.form method="post" enctype="multipart/form-data" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">

					<p>You can allocate students to groups using a spreadsheet.</p>

					<ol>
						<li><strong><a href="allocate/template">Download a template spreadsheet</a></strong>. This will be prefilled with the names and University ID numbers of students you have selected to be in Term 1 seminars. In Excel you may need to <a href="http://office.microsoft.com/en-gb/excel-help/what-is-protected-view-RZ101665538.aspx?CTT=1&section=7">exit protected view</a> to edit the spreadsheet.
						</li>
						<li><strong>Allocate students</strong> to groups using the dropdown menu in the <strong>Group name</strong> column or by pasting in a list of group names. The group names must match the groups you have already created for Term 1 seminars. The <strong>group_id</strong> field will be updated with a unique ID number for that group.
							You can select additional students to be in Term 1 seminars by entering their University ID numbers in the <strong>student_id</strong> column. Any students with an empty group_id field will be added to the list of students who haven't been allocated to a group.</li>
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

</#escape>