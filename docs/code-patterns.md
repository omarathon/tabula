# Code Patterns

Starting off with a single file here, but it should be broken up into sections if it gets a bit big.

This is a collaborative effort; everything's open for editing by anyone else.

## Server side

    def content = ???

### Testing

_Some stuff about making stuff testable, and avoiding having to load up the whole app._

## Client side

 - Javacript should be in external files only, and should not be included mid-page by your template.
 - Start off with no IDs, just classes, with data attributes for any custom settings/values. You might be surprised at how little you need unique identifiers.

### Reusable, data-driven components

Avoid Javascript that is written for a single page. Components should be generic and configurable. Even small in-page script tags to configure a generic component can be avoided. For example, take a sortable table plugin. You might put a few lines of script in the page to wire the plugin to a specific table ID. Then you add a custom function to tell it how to convert certain cells into machine readable values. Repeat until you have a bunch of script tags, possibly each with their own version of the same bug.

Better to make it data-driven, by telling it to wire in all tables that have a particular class, and provide configuration options via HTML5 data-* attributes on the table. Cell values that have a machine-readable value that's different from the HTML could specify it through a data attribute.

Don't scatter this sort of custom handling around:

	$('#extension-model').on('ajaxComplete', function(){
		$(this).find('details').details();
	})


Instead update the shared modal builder to apply the details polyfill to any modal - a form that doesn't use `details` elements won't be affected.

### Reusable UIs (FTL, LESS)

 - **Do not use IDs for styling!** Indeed as above, try without using IDs at all. Your widget is not unique. You may think it is a `#meeting-record-list-container` that needs its own style, but it is really just a list of collapsible items, and it should have a more generic class name with styles defined on that. Then if we should invent something that is visually similar to meeting records (say if we wanted to do something called "administrative notes") then it'd be easy to use the same classes and not have to update a bunch of styles and selectors.
 - **Macros!** Your HTML should probably be reusable, so it's a good idea to build it as such. If it's anything more than utterly trivial, why not make it a macro. If your HTML is completely unique in the app, then maybe we should check whether we're building an inconsistent UI. With macros everyone wins - we get to reuse stuff, and users get consistent UIs.
 - **Style guide!** When you've created a reusable widget, consider adding an example usage to `freemarker/style/view.ftl` so that it can be viewed by visiting `/style`.