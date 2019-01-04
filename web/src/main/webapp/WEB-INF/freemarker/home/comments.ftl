<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#macro webTeamForm>
	<@bs3form.labelled_form_group path="name" labelText="Your name">
		<@f.input path="name" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="email" labelText="Your email">
		<@f.input path="email" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="usercode" labelText="Usercode">
		<@f.input path="usercode" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="url" labelText="URL (where did you encounter a problem?)">
		<@f.input path="url" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="message" labelText="Your message">
		<@f.textarea path="message" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<input type="hidden" name="browser" value="" />
	<input type="hidden" name="os" value="" />
	<input type="hidden" name="resolution" value="" />
	<input type="hidden" name="ipAddress" value="" />
</#macro>

<h1>Need help?</h1>

<div class="row">
	<div class="col-md-9">

		<div class="boxstyle_ box3">
			<h2>Tabula manual</h2>

			<p>The manual provides guidance on how to use all components in Tabula.</p>

			<h4>Search for what you're trying to do</h4>

			<form class="search-manual" action="//search.warwick.ac.uk/sitebuilder" method="get">
				<input name="urlPrefix" value="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual" type="hidden">
				<input name="indexSection" value="sitebuilder" type="hidden">
				<div class="form-group">
					<div class="input-group input-group-lg">
						<input type="search" class="form-control" name="q" autocomplete="off" spellcheck="false" placeholder="Search manual" maxlength="640" />
						<span class="input-group-btn">
							<button class="btn btn-default" aria-label="Search" type="submit"><i class="fa fa-search"></i></button>
						</span>
					</div>
				</div>
				<ul class="results links"></ul>
			</form>

			<h4>Browse the manual by component</h4>

			<div class="container-fluid">
				<div class="row">
					<div class="col-sm-6">
						<p><a target="_blank" href="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual/administration-permissions/">Administration and permissions<i class="new-window-link" title="Link opens in a new window"></i></a></p>
						<p><a target="_blank" href="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual/cm2/">Coursework management<i class="new-window-link" title="Link opens in a new window"></i></a></p>
						<p><a target="_blank" href="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual/grids/">Exam grids<i class="new-window-link" title="Link opens in a new window"></i></a></p>
					</div>
					<div class="col-sm-6">
						<p><a target="_blank" href="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual/monitoring-points/">Monitoring points<i class="new-window-link" title="Link opens in a new window"></i></a></p>
						<p><a target="_blank" href="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual/profiles/">Profiles<i class="new-window-link" title="Link opens in a new window"></i></a></p>
						<p><a target="_blank" href="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual/small-group-teaching/">Small group teaching<i class="new-window-link" title="Link opens in a new window"></i></a></p>
					</div>
				</div>
			</div>
		</div>
		<div class="boxstyle_ box2">
			<h4>Need help with an Exam Grid?</h4>
			<p>If you need help with an exam grid for an area not covered in the Manual, please <a target="_blank" href="https://warwick.ac.uk/services/its/servicessupport/web/tabula/request-exam-grid-help">complete this web form</a></p>
		</div>

		<#if hasDeptAdmin>

			<h2>Contact your administrator</h2>
			<p>If you're having problems with Tabula, use the form below to contact your department's Tabula administrator.</p>

			<@f.form modelAttribute="command" action="${url('/help')}" cssClass="comment-form">
				<@bs3form.labelled_form_group path="name" labelText="Your name">
					<@f.input path="name" cssClass="form-control"/>
				</@bs3form.labelled_form_group>

				<@bs3form.labelled_form_group path="email" labelText="Your email">
					<@f.input path="email" cssClass="form-control"/>
				</@bs3form.labelled_form_group>

				<@bs3form.labelled_form_group path="usercode" labelText="Usercode">
					<@f.input path="usercode" cssClass="form-control"/>
				</@bs3form.labelled_form_group>

				<@bs3form.labelled_form_group path="message" labelText="Your message">
					<@f.textarea path="message" cssClass="form-control"/>
				</@bs3form.labelled_form_group>

				<input type="hidden" name="url" value="" />
				<button type="submit" class="btn btn-primary" name="recipient" value="${Recipients.DeptAdmin}">Send</button>
			</@f.form>

			<h2>Technical support</h2>
			<p>If you're experiencing technical issues with Tabula, please  <a href="#comment-modal" data-toggle="modal">contact the IT Services Web Team</a>.</p>

			<@f.form modelAttribute="command" action="${url('/help')}" cssClass="comment-form">

				<div class="modal fade" id="comment-modal">
					<@modal.wrapper>
						<@modal.header><h3 class="modal-title">Email the Web Team</h3></@modal.header>
						<@modal.body>

							<@webTeamForm />

						</@modal.body>
						<@modal.footer>
							<button type="submit" class="btn btn-primary" name="recipient" value="${Recipients.WebTeam}">Send message</button>
							<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
						</@modal.footer>
					</@modal.wrapper>
				</div>

			</@f.form>

		<#else>
			<h2>Technical support</h2>
			<p>If you're experiencing technical issues with Tabula, please contact the web team who can help with your query.</p>
			<@f.form modelAttribute="command" action="${url('/help')}" cssClass="comment-form">
				<@webTeamForm />
				<button type="submit" class="btn btn-primary" name="recipient" value="${Recipients.WebTeam}">Send</button>
			</@f.form>
		</#if>

	</div>

	<div class="col-md-3">
		<div class="boxstyle_ box2">
			<h4>Notice board</h4>
			<p>Visit the <a href="https://warwick.ac.uk/tabulainfo/forum">Tabula notice board</a> for updates on new developments, events and other news from the Tabula team.</p>
			<h4>Release notes</h4>
			<p>We regularly release updated versions of Tabula. Read the <a href="https://warwick.ac.uk/tabulainfo/releases">release notes</a> to learn about new features, improvements and bug fixes in each version.</p>
		</div>
		<div class="boxstyle_ box2">
			<h4>Request a feature</h4>
			<p>If there's something new you'd like to see in Tabula, please <a href="https://warwick.ac.uk/tabulainfo/request-feature">let us know</a>.</p>
		</div>
	</div>

</div>


<script>
	jQuery(function($){

		// save user agent
		BrowserDetect.init();
		var $form = $('.comment-form');
		$form.find('[name=os]').val(BrowserDetect.OS);
		$form.find('[name=browser]').val(BrowserDetect.browser + ' ' + BrowserDetect.version);
		$form.find('[name=resolution]').val(BrowserDetect.resolution);
		BrowserDetect.findIP(function(ip){
			$form.find('[name=ipAddress]').val(ip);
		});
		var $url = $form.find('[name=url]');
		if ($url.val() === '') {
			var currentPageArg = $.grep((window.location.search || '').substr(1).split('&'), function(pair) {
				return pair.split('=')[0] === 'currentPage';
			});
			if (currentPageArg.length > 0) {
				$url.val(currentPageArg[0].replace('currentPage=', ''));
			}
		}

		var manualSearch = null, $manualForm = $('form.search-manual');
		var manualSearchTypeahead = $manualForm.find('input[type=search]').tabulaTypeahead({
			source: function(query, process){
				// Abort any existing search
				if (manualSearch) {
					manualSearch.abort();
					manualSearch = null;
				}
				manualSearch = $.ajax({
					url: '//search.warwick.ac.uk/search/query.json?callback=?',
					dataType: 'json',
					data: {
						q: query + '*',
						urlPrefix: 'https://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual/',
						indexSection: 'sitebuilder'
					},
					success: function(data) {
						$('ul.results').html();
						process(data.results);
						if(data.totalHits> 8) {
							var link = data.link.split('/json').join('');
							$('ul.results').append(
								$('<li />')
									.append($('<a />').attr({'href': link, 'target': '_blank'}).html('More results'))
							);
						}
					}
				});
			}
		}).data('tabula-typeahead');

		manualSearchTypeahead.render = function (items) {
			var that = this;
			$.each(items, function (i, item) {
				that.$menu.append(
					$('<li />').append(
						$('<a />').attr({
							'href': item.link,
							'target': '_blank'
						}).html(item.title),
						' &nbsp; ',
						$('<span />').html(moment(item.lastModified).format('DD MMM YYYY'))
					)
				);
			});
			return this;
		};

		manualSearchTypeahead.show = function () {
			var $results = $manualForm.find('ul.results').empty();
			this.$menu.find('li').appendTo($results);
			this.shown = true;
			return this;
		};
	});
</script>

</#escape>