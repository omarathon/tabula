<#assign tiles=JspTaglibs["http://tiles.apache.org/tags-tiles"]>
<!doctype html>
<html lang="en-GB">
	<head>
		<#include "_head.ftl" />
	</head>
	<body class="horizontal-nav layout-100 coursework-page ${bodyClasses?default('')}">
		<div id="container">
			<#if (user.masquerading)!false >
			<div id="masquerade-notice">
			Masquerading as <strong>${user.apparentUser.fullName}</strong>. <a href="<@url page="/admin/masquerade"/>">Change</a>
			</div>
			</#if>
			<!-- Change this to header-medium or header-large as necessary - large is for homepages only -->
			<div id="header" class="header-small" data-type="image">
			
				<div id="masthead" class="transparent"> <!-- optional: class="transparent" -->
					<div class="access-info">
      					<a href="#main-content" accesskey="c" title="Skip to content [c]">Skip to content</a>
      					<a href="#navigation" accesskey="n" title="Skip to navigation [n]">Skip to navigation</a>
    				</div>
				
					<!-- Remove this if non-branded -->
						<!-- The on-hover class here specifies that the links should only be displayed on hover -->
						<div id="warwick-logo-container" class="on-hover">
							<a id="warwick-logo-link" href="http://www.warwick.ac.uk" title="University of Warwick homepage">
								<img id="warwick-logo" src="<@url resource="/static/images/logo.png" />" alt="University of Warwick">
							</a>
			
							<div id="warwick-site-links">
								<ul>
									<li><a href="http://www2.warwick.ac.uk/study">Study</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/research">Research</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/business">Business</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/alumni">Alumni</a></li>
									<li class="spacer">|</li>
									<li><a href="http://www2.warwick.ac.uk/newsandevents">News</a></li>
								</ul>
							</div>
						</div>
					<!-- End of removal if non-branded -->
					
					<div id="utility-container">
						<div id="utility-bar">
							<ul>
								<li>
								<#if user?? && user.loggedIn>
									Signed in as ${user.fullName}.
									<a class="sso-link" href="<@sso.logoutlink target="${rootUrl}/" />">Sign out</a>
								<#else>
								    <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
								</#if>
								</li>
							</ul>
						</div>
					
						<!-- Start removal here if non-branded -->
							<div id="search-container">
								<form action="http://search.warwick.ac.uk/website" class="form-inline">
									<ul class="index-section">
										<li data-index-section="website" data-index-title="Warwick" title="" class="active">Search University of Warwick</li>
										<li data-index-section="people" data-index-title="People" data-go-type="people" title="">Search for people at Warwick</li>
										<li data-index-section="blogs" data-index-title="Blogs" data-go-disabled="true" title="">Search Warwick Blogs</li>
										<li data-index-section="exampapers" data-index-title="Exam papers" data-go-disabled="true" title="">Search past exam papers</li>
										<li data-index-section="website" data-index-file-format="audio/;video/" data-index-title="Video" data-go-disabled="true" title="">Search video</li>
										<li class="more-link">
											<a href="http://search.warwick.ac.uk/website" title="View more search options">More&hellip;</a>
											<ul id="search-index-menu" style="display: none"></ul>
										</li>
									</ul>
									<input autocomplete="off" id="search-box" class="large" placeholder="Type your search here" name="q" /><input alt="Search" id="search-button" type="image" src="/static/images/shim.gif" title="Click here to search" />
									<div id="search-suggestions" style="display: none"></div>
								</form>
							</div>
						<!-- End of removal if non-branded -->
					</div>
				</div>
				
				<div id="page-header">
					<div class="content">
						<div id="site-header-container">
							<h1 id="site-header">
								<span id="parent-site-header"><a href="/" title="Tabula home page">Tabula</a></span>
								<span id="subsite-character">&raquo;</span>
								<span id="current-site-header"><a href="<@url page="/" />">Coursework management</a></span>
							</h1>
							
							<h2 id="strapline">
								
							</h2>
						</div>
						
						<div id="custom-header">
							<div class="">
							<#if (info.maintenance)!false>
								<span id="maintenance-mode-label" class="label label-warning" rel="popover" title="System read-only" data-placement="bottom" data-content="This system has been placed in a read-only mode. You will be able to downoad files, but other operations are not currently possible. Normal access will be restored very soon.">Read-only</span>
								<script>
									jQuery(function($){
										$('#maintenance-mode-label').popover();
									});
								</script>
							</#if>
							<#if user??>
								<#if user.sysadmin>
									<a class="btn btn-inverse" href="<@url page="/sysadmin/" />">Sysadmin</a>
								</#if>
								<#if user.masquerader || user.sysadmin>
									<a class="btn btn-inverse" href="<@url page="/admin/masquerade" />">Masquerade</a>
								</#if>
							</#if>
							</div>
						</div>
					</div>
				</div>
			</div>
			
			<div id="navigation-and-content">
			
	
	<div id="navigation" class="horizontal">
	<div id="primary-navigation-wrapper">
		<div id="before-primary-navigation"></div>
		
		<div id="primary-navigation-container" >
			<ul id="primary-navigation" >
				<li class="section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="<@url page="/" />">Home</a>										
						</div>
					</div>
				</li>
				<#if breadcrumbs??><#list breadcrumbs as crumb><li class="section rendered-link">
					<div class="link-content">
						<div class="title rendered-link-content">
							<a href="<@url page=crumb.url />" <#if crumb.tooltip??>title="${crumb.tooltip}"</#if>>${crumb.title}</a>										
						</div>
					</div>
				</li></#list></#if>
			</ul>
		</div>
		
		
		<div id="after-primary-navigation"></div>
	</div>
	
</div>
		
				<div id="content-wrapper">					     
					<div id="main-content">
						<#--
						<div id="page-title">
			
							<h1>
								<span id="after-page-title"></span>
							</h1>
			
							<div id="page-title-bottom"></div>
						</div>
						-->
			
						<!-- column-1 and column-2 may not stick around as IDs - don't use them in a site design -->
						<div id="column-1"><div id="column-1-content">
						
						<@tiles.insertAttribute name="body" />
						
						</div></div>

						<div style="clear:both;"></div>
					</div>
				</div>
			
				<div style="clear:both;"></div>
			</div>
			
			<div style="clear:both;"></div>
			
			<div id="footer">
				<div class="content">
					<div id="custom-footer">
						<!-- Enter any custom footer content here (like contact details et al) -->
					</div>
					
					<div style="clear:both;"></div>
					
					<div id="common-footer">
						<div id="page-footer-elements" class="nofollow">
							<span class="footer-left"></span> <span class="footer-right"></span>
							
							<div style="clear:both;"></div>
						</div>
						
						<div id="footer-utility">
							<ul>
								<li id="sign-inout-link">
				          			<#if user?? && user.loggedIn>
										<a class="sso-link" href="<@sso.logoutlink target="${rootUrl}/" />">Sign out</a>
									<#else>
									    <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
									</#if>
			          			</li>
			          			<li class="spacer">|</li>
			          			<li id="copyright-link"><a href="http://go.warwick.ac.uk/terms" title="Copyright Statement">&copy; <@warwick.copyright /></a></li>
			          			<li class="spacer">|</li>
			          			<li id="privacy-link"><a href="http://go.warwick.ac.uk/terms#privacy" title="Privacy statement">Privacy</a></li>
			          			<li class="spacer">|</li>
			          			<li id="accessibility-link"><a href="http://go.warwick.ac.uk/accessibility" title="Accessibility information [0]" accesskey="0">Accessibility</a></li>
			          			<li class="spacer subtle">|</li>
			          			<li class="subtle">
			          				App last built <@warwick.formatDate value=appBuildDate pattern="d MMMM yyyy HH:mm" />
			          			</li>
		          			</ul>
		          			
		          			<div id="app-feedback-link"><a href="/app/tell-us<#if info??>?currentPage=${info.requestedUri}</#if>">Give feedback</a></div>
		          				      					
	      					<div style="clear:both;"></div>
		          		</div>
					</div>
				</div>
			</div>
		</div>
		
	
		
	</body>
</html>
