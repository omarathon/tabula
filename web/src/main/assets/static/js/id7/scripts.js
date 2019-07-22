/* eslint-env browser */
import $ from 'jquery';
import initErrorReporter from '../errorreporter';

initErrorReporter();

const { jQuery, WPopupBox } = window;

/**
 * Cross-app scripting.
 * Only for code that is common across Tabula UI.
 * There are specific scripts for individual modules; use those for local code.
 */

window.Supports = {};
window.Supports.multipleFiles = !!('multiple' in (document.createElement('input')));

const exports = {};

// All WPopupBoxes will inherit this default configuration.
WPopupBox.defaultConfig = { imageroot: '/static/libs/popup/' };

// Tabula-specific rendition of tablesorter plugin for sortable tables
jQuery.fn.sortableTable = function sortableTable(settings) {
  const s = settings || {};

  const $table = $(this);
  if ($table.tablesorter) {
    const headerSettings = {};
    $('th', $table).each((index) => {
      const sortable = $(this).hasClass('sortable');
      const customSorter = $(this).data('sorter');
      if (!sortable) {
        headerSettings[index] = { sorter: false };
      } else if (customSorter) {
        headerSettings[index] = { sorter: customSorter };
      }
    });
    $table.tablesorter($.extend({ headers: headerSettings }, s));
  }
  return this;
};

function offsetEndDateTime($element) {
  if ($element.hasClass('startDateTime')) {
    const endDate = $element.data('datetimepicker').getDate().getTime() + parseInt($element.next('.endoffset').data('end-offset'), 10);
    const $endDateInput = $element.closest('.dateTimePair').find('.endDateTime');
    const endDatePicker = $endDateInput.data('datetimepicker');

    if ($endDateInput.length > 0) {
      endDatePicker.setDate(new Date(endDate));
      endDatePicker.setValue();
      $endDateInput.closest('.control-group').addClass('warning').removeClass('error');
    }
  } else if ($element.hasClass('endDateTime')) {
    $element.closest('.control-group').removeClass('warning');

    const $startDateInput = $element.closest('.dateTimePair').find('.startDateTime');

    // Check end time is later than start time
    if ($element.data('datetimepicker').getDate().getTime() < $startDateInput.data('datetimepicker').getDate().getTime()) {
      $element.closest('.control-group').addClass('error');
    } else {
      $element.closest('.control-group').removeClass('error');
    }
  }
}

function offsetEndDate($element) {
  if ($element.hasClass('startDateTime')) {
    const endDate = $element.data('datetimepicker').getDate().getTime() + parseInt($element.next('.endoffset').data('end-offset'), 10);
    const $endDateInput = $element.closest('.dateTimePair').find('.endDateTime');
    const endDatePicker = $endDateInput.data('datetimepicker');

    if ($endDateInput.length > 0) {
      endDatePicker.setDate(new Date(endDate));
      endDatePicker.setValue();
      $endDateInput.closest('.form-group').addClass('has-warning').removeClass('has-error');
    }
  } else if ($element.hasClass('endDateTime')) {
    $element.closest('.form-group').removeClass('warning');

    const $startDateInput = $element.closest('.dateTimePair').find('.startDateTime');

    // Check end time is later than start time
    if ($element.data('datetimepicker').getDate().getTime() < $startDateInput.data('datetimepicker').getDate().getTime()) {
      $element.closest('.form-group').addClass('has-error');
    } else {
      $element.closest('.form-group').removeClass('has-error');
    }
  }
}

// Tabula-specific rendition of date and date-time pickers
jQuery.fn.tabulaDateTimePicker = function tabulaDateTimePicker() {
  const $this = $(this);
  // if there is no datepicker bound to this input then add one
  if (!$this.data('datepicker')) {
    $this.datetimepicker({
      format: 'd-M-yyyy hh:ii:ss',
      weekStart: 1,
      minView: 'day',
      autoclose: true,
      fontAwesome: true,
      bootcssVer: 3,
    }).on('show', (ev) => {
      const d = new Date(ev.date.valueOf());
      const minutes = d.getUTCMinutes();
      const seconds = d.getUTCSeconds();
      const millis = d.getUTCMilliseconds();

      if (minutes > 0 || seconds > 0 || millis > 0) {
        d.setUTCMinutes(0);
        d.setUTCSeconds(0);
        d.setUTCMilliseconds(0);

        const { DPGlobal } = $.fn.datetimepicker;
        $(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat('dd-M-yyyy hh:ii:ss', 'standard'), 'en', 'standard'));

        $(this).datetimepicker('update');
      }
    }).next('.add-on')
      .css({ cursor: 'pointer' })
      .on('click', () => {
        $(this).prev('input').focus();
      });
  }

  $(this).on('changeDate', (e) => {
    offsetEndDateTime($(e.currentTarget));
  });
};

// 5-minute resolution
jQuery.fn.tabulaDateTimeMinutePicker = function tabulaDateTimeMinutePicker() {
  const $this = $(this);
  // if there is no datepicker bound to this input then add one
  if (!$this.data('datepicker')) {
    $this.datetimepicker({
      format: 'dd-M-yyyy hh:ii:ss',
      weekStart: 1,
      autoclose: true,
      fontAwesome: true,
      bootcssVer: 3,
    }).on('show', (ev) => {
      const d = new Date(ev.date.valueOf());
      const seconds = d.getUTCSeconds();
      const millis = d.getUTCMilliseconds();

      if (seconds > 0 || millis > 0) {
        d.setUTCSeconds(0);
        d.setUTCMilliseconds(0);

        const { DPGlobal } = $.fn.datetimepicker;
        $(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat('dd-M-yyyy hh:ii:ss', 'standard'), 'en', 'standard'));

        $(this).datetimepicker('update');
      }
    }).next('.add-on')
      .css({ cursor: 'pointer' })
      .on('click', () => {
        $(this).prev('input').focus();
      });
  }

  $(this).on('changeDate', (e) => {
    offsetEndDateTime($(e.currentTarget));
  });
};

jQuery.fn.tabulaDatePicker = function tabulaDatePicker() {
  const $this = $(this);
  // if there is no datepicker bound to this input then add one
  if (!$this.data('datepicker')) {
    $this.datetimepicker({
      format: 'dd-M-yyyy',
      weekStart: 1,
      minView: 'month',
      autoclose: true,
      fontAwesome: true,
      bootcssVer: 3,
    }).next('.add-on').css({ cursor: 'pointer' }).on('click', () => {
      $(this).prev('input').focus();
    });
  }

  $(this).on('changeDate', (e) => {
    offsetEndDate($(e.currentTarget));
  });
};

jQuery.fn.tabulaTimePicker = function tabulaTimePicker() {
  $(this).datetimepicker({
    format: 'hh:ii:ss',
    weekStart: 1,
    startView: 'day',
    maxView: 'day',
    autoclose: true,
    fontAwesome: true,
    bootcssVer: 3,
  }).on('show', (ev) => {
    const d = new Date(ev.date.valueOf());
    const seconds = d.getUTCSeconds();
    const millis = d.getUTCMilliseconds();

    if (seconds > 0 || millis > 0) {
      d.setUTCSeconds(0);
      d.setUTCMilliseconds(0);

      const { DPGlobal } = $.fn.datetimepicker;
      $(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat('hh:ii:ss', 'standard'), 'en', 'standard'));

      $(this).datetimepicker('update');
    }
  }).next('.add-on')
    .css({ cursor: 'pointer' })
    .on('click', () => {
      $(this).prev('input').focus();
    });

  $(this).on('changeDate', (e) => {
    offsetEndDateTime($(e.currentTarget));
  });
};

jQuery.fn.selectOffset = function selectOffset() {
  if ($(this).hasClass('startDateTime')) {
    $(this).on('click', () => {
      const indexValue = $(this).children(':selected').attr('value');
      $(this).closest('.dateTimePair')
        .find('.endDateTime')
        .attr('value', indexValue)
        .closest('.control-group')
        .addClass('warning');
    });
  }
};

/* apply to a checkbox or radio button. When the target is selected a div containing further
   related form elements is revealed.

   Triggers a 'tabula.slideMoreOptions.shown' event on the div when it is revealed and a
   'tabula.slideMoreOptions.hidden' event when it is hidden.
 */
jQuery.fn.slideMoreOptions = function slideMoreOptions($slidingDiv, showWhenChecked) {
  if ($(this).hasClass('slideMoreOptions-init')) {
    return;
  }
  $(this).addClass('slideMoreOptions-init');

  const $this = $(this);
  const name = $this.attr('name');
  const $form = $this.closest('form');

  const show = ($div, data) => {
    if (data === 'init') {
      $div.show(); // no animation on init
    } else {
      $div.stop().slideDown('fast', () => {
        $div.trigger('tabula.slideMoreOptions.shown');
      });
    }
  };

  const hide = ($div, data) => {
    if (data === 'init') {
      $div.hide(); // no animation on init
    } else {
      $div.stop().slideUp('fast', () => {
        $div.trigger('tabula.slideMoreOptions.hidden');
      });
    }
  };

  // for checkboxes, there will just be one target - the current element
  // it will have the same name as itself
  // for radio buttons, each radio button will be a target
  // they are identified as a group because they all have the same name
  const $changeTargets = $(`input[name='${name}']`, $form);
  if (showWhenChecked) {
    $changeTargets.change((event, data) => {
      if ($this.is(':checked')) show($slidingDiv, data);
      else hide($slidingDiv, data);
    });
  } else {
    $changeTargets.change((event, data) => {
      if ($this.is(':checked')) hide($slidingDiv, data);
      else show($slidingDiv, data);
    });
  }
  $this.trigger('change', 'init'); // pass 'init' to suppress animation on load.
};


// submit bootstrap form using Ajax
jQuery.fn.tabulaAjaxSubmit = function tabulaAjaxSubmit(successCallback) {
  if ($(this).hasClass('tabulaAjaxSubmit-init')) return;
  $(this).addClass('tabulaAjaxSubmit-init');

  const errorHandler = ($form, data) => {
    const scopeSelector = (data.formId !== undefined) ? `#${data.formId} ` : '';
    if ($form.is('.double-submit-protection')) {
      $form.find('.submit-buttons .btn').removeClass('disabled');
      $form.removeData('submitOnceSubmitted');
    }
    $form.find('.spinnable').each((i, el) => {
      const $spinnable = $(el);
      if ($spinnable.data('spinContainer')) {
        $spinnable.data('spinContainer').spin(false);
      }

      if (window.pendingSpinner !== undefined) {
        clearTimeout(window.pendingSpinner);
        window.pendingSpinner = null;
      }
    });

    // delete any old errors
    $(`${scopeSelector}div.error`).remove();
    $(`${scopeSelector}.has-error`).removeClass('has-error');

    Object.keys(data.result).forEach((err) => {
      if (Object.prototype.hasOwnProperty.call(data.result, 'error')) {
        const message = data.result[err];
        const inputSelector = `${scopeSelector}input[name=${err}]`;
        const textareaSelector = `${scopeSelector}textarea[name=${err}]`;

        const $field = $(`${inputSelector}, ${textareaSelector}`);
        $field.closest('.form-group').addClass('has-error');

        // insert error message
        $field.last().after(`<div class="error help-block">${message}</div>`);
      }
    });

    // Handle global errors
    $(scopeSelector).find('.alert-danger').remove();
    Object.keys(data.errors).forEach((err) => {
      if (Object.prototype.hasOwnProperty.call(data.errors, 'error')) {
        $(scopeSelector).prepend($('<div class="alert alert-danger" />').text(data.errors[err].message));
      }
    });
  };

  $(this).on('submit', 'form', (e) => {
    e.preventDefault();
    const $form = $(e.currentTarget).trigger('tabula.ajaxSubmit');
    $.post($form.attr('action'), $form.serialize(), (data) => {
      if (data.status === 'error') {
        errorHandler($form, data);
      } else {
        successCallback(data);
      }
    }).fail((response) => {
      errorHandler($form, response.responseJSON);
    });
  });
};


/*
 * Prepare a spinner and store reference in data store.
 * Add spinner-* classes to control positioning and automatic spinning
 *
 * Otherwise methods from spin.js to instantiate, eg:
 * $(el).data('spinContainer').spin('small');
 * $(el).data('spinContainer').spin(false);
 */
jQuery.fn.tabulaPrepareSpinners = function tabulaPrepareSpinners(s) {
  const selector = s || '.spinnable';

  // filter selector and descendants
  const $spinnable = $(this).find(selector).add($(this).filter(selector));

  if ($spinnable.length) {
    // stop any delayed spinner
    if (window.pendingSpinner !== undefined) {
      clearTimeout(window.pendingSpinner);
      window.pendingSpinner = null;
    }

    $spinnable.each((i, el) => {
      const $this = $(el);

      if ($this.data('spinContainer')) {
        // turn off any existing spinners
        $this.data('spinContainer').spin(false);
      } else {
        // create new spinner element
        const $spinner = $('<div class="spinner-container" />');

        // position new spinner
        if ($this.is('.spinner-prepend')) {
          $this.prepend($spinner);
        } else if ($this.is('.spinner-append')) {
          $this.append($spinner);
        } else if ($this.is('.spinner-before')) {
          $this.before($spinner);
        } else if ($this.is('.spinner-after')) {
          $this.after($spinner);
        } else {
          // default centred on element itself
          $spinner.remove();
          $this.data('spinContainer', $this);
        }

        if (!$this.data('spinContainer')) {
          // if not yet stored...
          $this.data('spinContainer', $spinner);
        }

        if ($this.is('.spinner-auto')) {
          // spin only after 500ms
          $this.click(() => {
            if (!$this.is('.disabled')) {
              const $container = $this.data('spinContainer');
              window.pendingSpinner = setTimeout(() => {
                $container.spin('small');
              }, 500);
            }
          });
        }
      }
    });
  }
};


/*
 * .double-submit-protection class on a form will detect submission
 * and prevent submitting twice. It will also visually disable any
 * .btn items in a .submit-buttons container.
 *
 * Obviously this won't make it impossible to submit twice, if JS is
 * disabled or altered.
 */
jQuery.fn.tabulaSubmitOnce = function tabulaSubmitOnce() {
  const $this = $(this);

  if ($this.is('form') && !$this.data('submitOnceHandled')) {
    $this.data('submitOnceHandled', true);
    $this.removeData('submitOnceSubmitted');

    $this.on('submit tabula.ajaxSubmit', (event) => {
      const $target = $(event.currentTarget);
      const submitted = $target.data('submitOnceSubmitted');

      if (!submitted) {
        const $buttons = $target.find('.submit-buttons .btn').not('.disabled');
        $buttons.addClass('disabled');
        $target.data('submitOnceSubmitted', true);
        // For FF and other browsers with BFCache/History Cache,
        // re-enable the form if you click Back.
        $(window).on('pageshow', () => {
          $buttons.removeClass('disabled');
          $target.removeData('submitOnceSubmitted');
        });
        return true;
      }
      event.preventDefault();
      return false;
    });
  }
};


/*
 Customised Popover wrapper. Implements click away to dismiss.
 */
$.fn.tabulaPopover = function tabulaPopover(options) {
  let $items = this;
  const initClass = 'tabulaPopover-init';

  // filter already initialized popovers
  $items = $items.not(initClass);

  const tooltipItems = $items.filter('.use-tooltip').toArray(); // boostrap js based tooltip

  // set options, with defaults
  const defaults = {
    template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
    sanitize: false,
  };
  const opts = $.extend({}, defaults, options);


  $items.on('click', (e) => {
    const $target = $(e.currentTarget);
    $target.tooltip('disable');
    $target.trigger('mouseout');

    // don't popover disabled
    if ($target.hasClass('disabled')) {
      e.stopImmediatePropagation();
    }
    // Prevent propagation of click event to parent DOM elements
    e.preventDefault();
    e.stopPropagation();
  });

  // TAB-2920
  $items.on('hidden', (e) => {
    e.stopPropagation();
  });

  // Click away to dismiss
  $('html').on('click.popoverDismiss', (e) => {
    // if clicking anywhere other than the popover itself
    if ($(e.currentTarget).closest('.popover').length === 0 && $(e.currentTarget).closest('.use-popover').length === 0) {
      $('.popover-inner').find('button.close').click();
      for (let i = 0; i < tooltipItems.length; i += 1) {
        $(tooltipItems[i]).tooltip('enable');
      }
    }
  });

  // TAB-945 support popovers within fix-on-scroll
  $items.closest('.fix-on-scroll').on('fixed', () => {
    // Re-position any currently shown popover whenever we trigger a change in fix behaviour
    $items.each((i, el) => {
      const $item = $(el);
      const popover = $item.popover().data('bs.popover');
      const $tip = popover.tip();
      if ($tip.is(':visible')) {
        // Re-position. BUT HOW?
        $item.popover('show');
      }
    });
  });

  /* SPECIAL: popovers don't inherently know their progenitor, yet popover methods
   * (eg. hide) are *only* callable on *that original element*. So to close
   * a specific popover (or introductory) programmatically you need to jump hoops.
   * Lame.
   *
   * Workaround is to handle the shown event on the calling element,
   * call its popover() method again to get an object reference and then go diving
   * for a reference to the new popover itself in the DOM.
   */
  $items.on('shown.bs.popover', (e) => {
    const $po = $(e.currentTarget).popover().data('bs.popover').tip();
    $po.data('creator', $(e.currentTarget));
  });
  $('body').on('click', '.popover .close', (e) => {
    const $creator = $(e.currentTarget).parents('.popover').data('creator');
    if ($creator) {
      $creator.popover('hide');
      $creator.tooltip('enable');
    }
  });

  // now that's all done, bind the popover
  $items.each((i, el) => {
    // allow each popover to override the container via a data attribute
    $(el).popover($.extend({}, opts, {
      container: $(el).data('container'),
      trigger: $(el).data('trigger'),
    })).addClass(initClass);
  });

  // ensure popovers/introductorys override title with data-title attribute where available
  $items.each((i, el) => {
    if ($(el).attr('data-title')) {
      $(el).attr('data-original-title', $(el).attr('data-title'));
    }
  });

  return $items;
};

/*
 Invoke on .nav-tabs to overflow items into a dropdown
 instead of onto another row.
 */
jQuery.fn.tabOverflow = function tabOverflow() {
  const selector = '.nav-tabs';
  let $target = $(this).find(selector).add($(this).filter(selector));
  const initClass = `${selector}-overflow-inited`;
  // filter already initialized tabs
  $target = $target.not(initClass);

  // Packs the given list items into a dropdown.
  const overflowThese = ($items) => {
    const $dropdown = $('<li>').addClass('dropdown').addClass('pull-right');
    const $link = $('<a>').addClass('dropdown-toggle').html('More...');
    const $caret = $('<b>').addClass('caret');
    const $ul = $('<ul>', { class: 'dropdown-menu' });
    $dropdown
      .append($ul)
      .append($link.append($caret));
    $items.first().before($dropdown);
    $ul.append($items);
    $link.dropdown();
  };

  function overflow($items) {
    $items.each((i, e) => {
      let x = 0;
      // find the first child that hits a new line by comparing leftness.
      $(e).children().each((j, child) => {
        const { left } = $(child).position();
        if (left < x) {
          // The first prev() is so nextAll includes child;
          // The second prev() is a silly hack to put an extra
          // item into the menu to make way for the dropdown item;
          // not very scientific. Could measure required width?
          overflowThese($(child).prev().prev().nextAll());
          return false;
        }
        x = left;
        return true;
      });
    });
  }

  function dropflow($items) {
    // remove the dropdown nature
    $items.find('.dropdown-menu li').unwrap();
    $items.find('.dropdown li').unwrap();
    $items.find('.dropdown-toggle').remove();
  }

  function reflow($items) {
    // convenience method
    dropflow($items);
    overflow($items);
  }

  // overflow on init
  overflow($target);

  // on click, move active tab to head, and reflow
  this.on('click', '.dropdown-menu li', (e) => {
    const $tabs = $(e.currentTarget).closest(selector);
    $tabs.prepend($(e.currentTarget));
    reflow($tabs);
  });

  // on tabbable sort or custom change, reflow
  this.on('tabbablechanged sortstop', (e) => {
    const $tabs = $(e.currentTarget).find(selector).add($(e.currentTarget).filter(selector));
    reflow($tabs);
    // if active item pushed into dropdown, try again
    const hiddenActiveTab = $tabs.find('.dropdown-menu .active');
    if (hiddenActiveTab.length) {
      $tabs.prepend(hiddenActiveTab);
      reflow($tabs);
    }
  });

  // tidy up and return for chaining
  $target.addClass(initClass);
  return this;
};

// collapsible striped section
// exported so can be called on-demand e.g. after an ajax-load
// adds a class to prevent double-init
exports.initCollapsible = ($el) => {
  const $element = (typeof ($el) === 'undefined') ? $('.striped-section.collapsible') : $el;
  $element.filter(':not(.collapsible-init)').each((i, el) => {
    const $section = $(el).addClass('collapsible-init');
    const checkboxToggle = $section.hasClass('checkbox-toggle');
    let $icon = $('<i />');
    const open = () => $section.hasClass('expanded');
    const $title = $section.find('.section-title');

    if (!checkboxToggle) {
      let toTrim;
      if (open()) $icon.addClass('fa fa-fw fa-chevron-down');
      else $icon.addClass('fa fa-fw fa-chevron-right');
      if ($title.find('.icon-container').length) {
        $title.find('.icon-container').first().prepend($icon);
        if ($icon.get(0).nextSibling === null) {
          toTrim = $title.find('.icon-container').first().get(0).nextSibling;
        } else {
          toTrim = $icon.get(0).nextSibling;
        }
      } else {
        $title.prepend($icon);
        toTrim = $icon.get(0).nextSibling;
      }
      if (toTrim !== null) {
        toTrim.textContent = $.trim(toTrim.textContent);
      }
    }

    let populateContent = (onComplete) => {
      onComplete();
    };
    if ($section.data('populate') && $section.data('href')) {
      $section.data('loaded', false).data('loading', false);
      let formdata = { ts: new Date().getTime() };
      if ($section.data('form')) {
        formdata = $($section.data('form')).serialize();
      }

      // Populate function
      populateContent = (onComplete) => {
        if ($section.data('loaded')) onComplete();
        else if ($section.data('loading')) {
          // prevent multiple invocation
        } else {
          $section.data('loading', true);
          $icon.removeClass().addClass('fa fa-fw fa-refresh fa-spin');

          const $target = $section.find($section.data('populate'));

          $.post(
            $section.data('href'),
            formdata,
            (html) => {
              $target.html(html);
              $target.find('a.ajax-modal').ajaxModalLink();
              $target.find('.use-tooltip').tooltip({ sanitize: false });
              $target.find('.use-popover').tabulaPopover({
                trigger: 'click',
                container: 'body',
              });
              onComplete();
              $section.data('loading', false).data('loaded', true).trigger('loaded.collapsible');
            },
          );
        }
      };
    }

    if (checkboxToggle) {
      const $checkbox = $section.find('input.toggle-collapsible');
      $checkbox.on('change', (e) => {
        if ($(e.currentTarget).is(':checked')) {
          $section.addClass('expanded');
          $checkbox.parent().find('.toggle-collapsible-on').removeClass('hidden');
          $checkbox.parent().find('.toggle-collapsible-off').addClass('hidden');
        } else {
          $section.removeClass('expanded');
          $checkbox.parent().find('.toggle-collapsible-on').addClass('hidden');
          $checkbox.parent().find('.toggle-collapsible-off').removeClass('hidden');
        }
      });
    } else {
      // Begin view polyfill
      // Do not rely on this, fix the templates instead.
      $title.each(() => {
        const $titleElement = $(this);

        if ($titleElement.find('a').length === 0) {
          $titleElement.removeAttr('tabindex');
          const $replacementLink = $('<a>').addClass('collapse-trigger icon-container');
          $replacementLink.attr('href', '#');
          $replacementLink.html($titleElement.html());
          $replacementLink.contents()
            .filter(() => this.nodeType !== 1)
            .wrap('<span class="collapse-label">');
          $replacementLink.find('span.collapse-label').text($.trim($replacementLink.find('span.collapse-label').text()));
          $titleElement.html('');
          $titleElement.prepend($replacementLink);
        }
      });
      // End view polyfill
      $title.find('a.collapse-trigger').on('click', (e) => {
        // Ignore clicks where we are clearing a dropdown
        if ($(e.currentTarget).parent().find('.dropdown-menu').is(':visible')) {
          return;
        }

        const $eventTarget = $(e.target);
        if (!$(e.currentTarget).hasClass('collapse-trigger') && ($eventTarget.is('a, button') || $eventTarget.closest('a, button').length)) {
          // Ignore if we're clicking a button
          return;
        }

        $icon = $(e.currentTarget).find('i').filter('.fa,.fal,.far,.fas,.fab').first();
        e.preventDefault();

        if (open()) {
          $section.removeClass('expanded');
          $icon.removeClass().addClass('fa fa-fw fa-chevron-right');
          $(e.currentTarget).attr('aria-expanded', 'false');
        } else {
          populateContent(() => {
            $section.addClass('expanded');
            $(e.currentTarget).attr('aria-expanded', 'true');
            $icon.removeClass().addClass('fa fa-fw fa-chevron-down');

            if ($section.data('name')) {
              // Use history.pushState here if supported as it stops the page jumping
              if (window.history && window.history.pushState && window.location.hash !== `#${$section.data('name')}`) {
                window.history.pushState({}, document.title, `${window.location.pathname}${window.location.search}#${$section.data('name')}`);
              } else {
                window.location.hash = $section.data('name');
              }
            }

            $section.wideTables();
          });
        }

        $(window).trigger('resize');
      });

      if (!open() && window.location.hash && window.location.hash.substring(1) === $section.data('name')) {
        // simulate a click
        $title.trigger('click');
      }
    }
  });
};

exports.resizeModalIframes = (height) => {
  // Adding extra height for 'browser knows iframe size' purposes
  $('.modal-body > iframe').height(height + 36);
};

exports.setArgOnUrl = (url, argName, argValue) => {
  if (url.indexOf('?') === -1) {
    return `${url}?${argName}=${argValue}`;
  }
  const args = url.substring(url.indexOf('?') + 1, url.length).split('&');
  let found = false;
  const newArgs = $.map(args, (pair) => {
    const arg = pair.split('=');
    if (arg[0] === argName) {
      found = true;
      return `${argName}=${argValue}`;
    }
    return pair;
  });
  if (!found) {
    newArgs.push(`${argName}=${argValue}`);
  }
  return `${url.substring(0, url.indexOf('?'))}?${newArgs.join('&')}`;
};

exports.scrollableTableSetup = () => {
  $('.scrollable-table .right').find('.table-responsive').on('scroll', (e) => {
    const $this = $(e.currentTarget);
    if ($this.scrollLeft() > 0) {
      $this.parent(':not(.left-shadow)').addClass('left-shadow');
    } else {
      $this.parent().removeClass('left-shadow');
    }
  });
};

exports.tableSortMatching = (tableArray) => {
  const matchSorting = ($sourceTable, targetTables) => {
    const $sourceRows = $sourceTable.find('tbody tr');
    $.each(targetTables, (i, $table) => {
      const $tbody = $table.find('tbody');
      const oldRows = $tbody.find('tr').detach();
      $.each($sourceRows, (j, row) => {
        const $sourceRow = $(row);
        oldRows.filter((idx, node) => $(node).data('sortId') === $sourceRow.data('sortId')).appendTo($tbody);
      });
    });
  };

  if (tableArray.length < 2) return;

  $.each(tableArray, (i) => {
    const otherTables = tableArray.slice();
    otherTables.splice(i, 1);
    this.on('sortEnd', (e) => {
      matchSorting($(e.currentTarget), otherTables);
    }).find('tbody tr').each((idx, tr) => {
      $(tr).data('sortId', idx);
    });
  });
};

// on ready
$(() => {
  // form behavioural hooks
  $('input.date-time-picker').tabulaDateTimePicker();
  $('input.date-picker').tabulaDatePicker();
  $('input.time-picker').tabulaTimePicker();
  $('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
  $('form.double-submit-protection').tabulaSubmitOnce();
  $('select.selectOffset').selectOffset();

  const $body = $('body');
  // prepare spinnable elements
  $body.tabulaPrepareSpinners();

  // repeat these hooks for modals when shown
  $body.on('shown.bs.modal', (e) => {
    const $m = $(e.currentTarget);
    $m.find('input.date-time-picker').tabulaDateTimePicker();
    $m.find('input.date-picker').tabulaDatePicker();
    $m.find('input.time-picker').tabulaTimePicker();
    $m.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
    $m.find('form.double-submit-protection').tabulaSubmitOnce();
    $m.find('select.selectOffset').selectOffset();
    $m.tabulaPrepareSpinners();

    const $form = ($m.find('iframe').contents().find('form').length === 1) ? $m.find('iframe').contents().find('form') : $m.find('form');
    if ($form.length === 1 && !$form.hasClass('dirty-check-ignore')) {
      $form.areYouSure();

      $m.find('[data-dismiss="modal"]').on('click', (ev) => {
        $form.trigger('checkForm.areYouSure');
        // eslint-disable-next-line no-alert
        if ($form.hasClass('dirty') && !window.confirm('You have unsaved changes! \n\n Are you sure you want to close this form?')) {
          ev.preventDefault();
          ev.stopImmediatePropagation();
        } else {
          $form.trigger('reset');
        }
      });

      // Prevent modal closing from clicking on parent page or from cancel and cross buttons
      // (use dirty check event above).
      $m.find('[data-dismiss="modal"]').off('click.dismiss.modal');
      $('div.modal-backdrop.fade.in').off();
    }
  });

  // TAB-4210 http://stackoverflow.com/questions/27371918/stacking-modals-scrolls-the-main-page-when-one-is-closed
  $body.on('hidden.bs.modal', () => {
    // If there are other open modals, re-add the modal-open class to the body
    if ($('.modal').hasClass('in')) {
      $body.addClass('modal-open');
    }
  });

  $(document).on('ajaxComplete', (e, xhr) => {
    if (xhr.responseText && xhr.responseText.indexOf('<details') !== -1) {
      $('details').details();
    }
  });

  /* When a .long-running link is clicked it will be
   * replaced with "Please wait" text, to tell the user to expect to
   * wait a few seconds.
   */
  $('a.long-running, button.long-running').click((event) => {
    const $this = $(event.currentTarget);
    const originalText = $this.html();
    if (!$this.hasClass('clicked') && !$this.hasClass('disabled') && !$this.parent().hasClass('disabled')) {
      $this.addClass('clicked').css({ opacity: 0.5 }).width($this.width()).html('Please wait&hellip;');
      setTimeout(() => {
        $this.removeClass('clicked').css({ opacity: 1 }).html(originalText);
      }, 5000);
      return true;
    }
    event.preventDefault();
    return false;
  });

  $('a.copyable-url').copyable({ prefixLinkText: true }).tooltip({ sanitize: false });

  // add .use-tooltip class and title attribute to enable cool looking tooltips.
  // http://twitter.github.com/bootstrap/javascript.html#tooltips
  $('.use-tooltip[title]:not([title=""])').tooltip({ sanitize: false });

  // add .use-popover and optional data- attributes to enable a cool popover.
  // http://twitter.github.com/bootstrap/javascript.html#popovers
  $('.use-popover').tabulaPopover({
    trigger: 'click',
    container: '.id7-fixed-width-container',
  });

  $('.use-wide-popover').tabulaPopover({
    trigger: 'click',
    container: '.id7-fixed-width-container',
    template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
  });

  // add .use-introductory for custom popover.
  // https://github.com/twitter/bootstrap/issues/2234
  $('.use-introductory').tabulaPopover({
    trigger: 'click',
    container: '.id7-fixed-width-container',
    template: '<div class="popover introductory"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div><div class="footer"><form class="form-inline"><label><input type="checkbox"> Don\'t show me this again</label></form></div></div></div>',
  });

  $('.use-introductory:not(.auto)').each((i, el) => {
    const { template } = $(el).data('bs.popover').options;
    $(el).data('bs.popover').options.template = template.replace('<input type="checkbox">', '<input type="checkbox" checked="checked">');
  });

  // auto-show introductory popover on load, based on class
  $('.use-introductory.auto').popover('show');
  // persist introductory popover auto-show state
  $('.id7-fixed-width-container').on('change', '.introductory .footer input', (e) => {
    // If intro text is changed to reflect new features
    // its hash should change to ensure end users see the new version
    const hash = $(e.currentTarget).parents('.introductory').data('creator').data('hash');
    // use this hook to persist showOnLoad state with some ajax shizzle
    $.post(`/settings/showIntro/${hash}`, { dismiss: $(e.currentTarget).is(':checked') });
  });

  /* details/summary polyfill
   * https://github.com/mathiasbynens/jquery-details
   * WARNING: apply method ONLY to details tags.
   * Call it on other elements for UI hilarity.
   */
  $('html').addClass($.fn.details.support ? 'details' : 'no-details');
  $('details').details();
  // different selector for open details depending on if it's native or polyfill.
  const openSlctr = $.fn.details.support ? '[open]' : '.open';

  // togglers - relies on everything being in a section element
  const $tabulaPage = $('.tabula-page');
  $tabulaPage.on('click', '.open-all-details', (e) => {
    const $container = $(e.currentTarget).closest('section');
    $container.find(`details:not(${openSlctr}) summary`).click();
    $container.find('.open-all-details').hide();
    $container.find('.close-all-details').show();
  });
  $tabulaPage.on('click', '.close-all-details', (e) => {
    const $container = $(e.currentTarget).closest('section');
    $container.find(`details${openSlctr} summary`).click();
    $container.find('.close-all-details').hide();
    $container.find('.open-all-details').show();
  });
  $tabulaPage.find('section .close-all-details').hide();

  exports.initCollapsible();

  // Form dirty checking
  $('form.dirty-check').areYouSure({ addRemoveFieldsMarksDirty: true, renamedFieldsMarksDirty: true });
  $('.dirty-check-ignore').on('click', () => {
    $('form.dirty-check').trigger('reinitialize.areYouSure');
  });

  // If we're on OS X, replace all kbd.keyboard-control-key with Cmd instead of Ctrl
  if (navigator.platform.indexOf('Mac') !== -1) {
    $('kbd.keyboard-control-key').html('<span class="mac-cmd">&#8984;</span> cmd');
  }

  // Fixed to top on scroll
  if ($('.fix-on-scroll').length) {
    const gutter = $('#navigation').height();

    $(window).scroll((e) => {
      const scrollTop = $(e.currentTarget).scrollTop() + gutter;

      if (!$('body.is-smallscreen').length) {
        $('.fix-on-scroll:visible').each((i, el) => {
          const $this = $(el);

          let $scrollContainer = $this.closest('.fix-on-scroll-container');
          if ($scrollContainer.length === 0) $scrollContainer = $('body');

          const height = $this.height();
          const maxHeight = $(window).height() - gutter;
          const tooHigh = (height > maxHeight);

          const floor = $scrollContainer.offset().top + $scrollContainer.height();

          const isFixed = $this.data('is-fixed');
          const pinnedToFloor = $this.data('is-pinned-to-floor');

          const offsetTop = (isFixed) ? $this.data('original-offset') : $this.offset().top;
          const pinToFloor = (scrollTop + height) > floor;

          if (!tooHigh && scrollTop > offsetTop && !isFixed) {
            // Fix it
            $this.data('original-offset', offsetTop);
            $this.data('original-width', $this.css('width'));
            $this.data('original-position', $this.css('position'));
            $this.data('original-top', $this.css('top'));

            $this.css({
              width: $this.width(),
              position: 'fixed',
              top: gutter,
            });

            $this.data('is-fixed', true);
            $this.trigger('fixed', [true, 'top']);
          } else if (!tooHigh && isFixed && pinToFloor) {
            // Pin to the floor
            const diff = (scrollTop + height) - floor;

            $this.css('top', gutter - diff);
            $this.data('is-pinned-to-floor', true);
            $this.trigger('fixed', [true, 'bottom']);
          } else if (!tooHigh && isFixed && !pinToFloor && pinnedToFloor) {
            // Un-pin from the floor
            $this.css('top', gutter);
            $this.data('is-pinned-to-floor', false);
            $this.trigger('fixed', [true, 'top']);
          } else if ((tooHigh || scrollTop <= offsetTop) && isFixed) {
            // Un-fix it
            $this.css('width', $this.data('original-width'));
            $this.css('position', $this.data('original-position'));
            $this.css('top', $this.data('original-top'));

            $this.data('is-fixed', false);
            $this.trigger('fixed', [false]);
          }
        });
      }
    });
  }

  // tabbable-gadgety-listy things
  const $t = $('.tabbable');
  const $panes = $t.find('.panes');

  if ($t.length && $panes.length) {
    // set up layout control
    const $lt = $('<span class="layout-tools pull-right muted"><i class="icon-folder-close hidden-phone" title="Switch to tabbed layout"></i> <i class="icon-th-large" title="Switch to gadget layout"></i> <i class="icon-reorder" title="Switch to list layout"></i><!-- <i class="icon-ok" title="Save layout settings"></i>--></span>');
    $t.prepend($lt);
    $t.trigger('tabbablechanged');

    const reset = () => { // to list
      $t.hide();
      const $cols = $t.find('.cols');
      $cols.find('.gadget').appendTo($panes);
      $cols.remove();
      $t.find('.agent').removeClass('span4');
      $t.find('.gadget-only').children().unwrap();
      $t.find('.tab-container').remove();
      $t.find('.gadget, .tab-content, .tab-pane, .active').removeClass('gadget tab-content tab-pane active');
    };

    $(document).on('tabbablechanged', (e, options) => {
      $('.tooltip').remove();
      $t.show().find('.tab-container i, .layout-tools i').tooltip({ sanitize: false });
      if (typeof (options) === 'object' && typeof (options.callback) === typeof (Function)) options.callback();
    });

    // layout options
    const tabLayout = () => { // tabify
      reset();
      const $tabContainer = $('<div class="row-fluid tab-container"><ul class="nav nav-tabs"></ul></div>');
      const $tabs = $tabContainer.find('ul');
      $panes.children('li').each((i, el) => {
        let title = $(el).attr('data-title');
        if (!title) {
          title = $(el).find('h4').html();
        }
        const link = `#${$(el).attr('id')}`;
        const $tab = $(`<li><a href="${link}" data-toggle="tab" data-title="${title}"><span class="title">${title}</span> <i class="icon-move" title="Click and drag to move"></i> <i class="icon-resize-small" title="Collapse"></i></a></li>`);
        $tabs.append($tab);
      });
      $lt.after($tabContainer);
      $panes.addClass('tab-content').children().addClass('tab-pane');
      $t.find('.nav-tabs').sortable({
        handle: '.icon-move',
        placeholder: 'tabbable-placeholder',
      }).show()
        .find('li:first > a')
        .tab('show');
      $t.trigger('tabbablechanged', {
        callback: () => $('.tabbable').tabOverflow(),
        layout: 'tabbed',
      });
    };
    $t.on('click', '.layout-tools .icon-folder-close', tabLayout);

    const gadgetLayout = () => { // gadgetify
      reset();
      const $cols = $('<div class="cols row-fluid"><ol class="ex-panes span6" /><ol class="ex-panes span6" /></div>');
      const paneCount = $panes.children('li').length;
      $t.append($cols);
      $panes.children('li').each((idx, el) => {
        const $element = $(el);
        const $gadget = $element.addClass('gadget');
        const title = $element.attr('data-title') || $element.find('h4').first().text();
        const link = `#${$element.attr('id')}`;
        const $tab = $(`<li><a href="${link}" data-toggle="tab" data-title="${title}" title="Click and drag to move"><span class="title">${title}</span> <i class="icon-minus-sign-alt" title="Hide ${title}"></i></a></li>`);
        const $gadgetHeaderTab = $('<div class="row-fluid tab-container"><ul class="nav nav-tabs"></ul></div>');
        $element.find('.agent').removeClass('span4');
        $gadgetHeaderTab.children().append($tab);
        $gadget.wrapInner('<div class="tab-content gadget-only" />').children().wrapInner('<div class="gadget-only tab-pane active" />');
        $gadget.prepend($gadgetHeaderTab).find('.tab-container li > a').tab('show');

        // populate columns (dumbly)
        $element.appendTo(idx < paneCount / 2 ? $cols.children().first() : $cols.children().last());
      });

      // make sortable & finish up rendering
      $t.find('.ex-panes').sortable({
        handle: '.tab-container a',
        placeholder: 'sort-target',
        forcePlaceholderSize: true,
        connectWith: '.span6',
      });

      $t.trigger('tabbablechanged', { layout: 'gadget' });
    };
    $t.on('click', '.layout-tools .icon-th-large', gadgetLayout);

    const listLayout = () => { // listify
      reset();
      $t.trigger('tabbablechanged', { layout: 'list' });
    };
    $t.on('click', '.layout-tools .icon-reorder', listLayout);

    // tab controls
    $t.on('click', '.tab-container .icon-resize-small', (e) => {
      e.stopPropagation();
      const $a = $(e.currentTarget).parent();
      const title = $a.data('title');
      $(e.currentTarget).attr('title', `Expand ${title}`);
      $a.data('href', $a.attr('href'))
        .removeAttr('href')
        .removeAttr('data-toggle')
        .html(
          $a.html()
            .replace(title, '')
            .replace('resize-small', 'resize-full'),
        )
        .addClass('disabled');
      $t.trigger('tabbablechanged');
    });

    $t.on('click', '.tab-container .icon-resize-full', (e) => {
      e.stopPropagation();
      const $a = $(e.currentTarget).parent();
      const title = $a.data('title');
      $(e.currentTarget).attr('title', 'Collapse');
      $a.attr('href', $a.data('href'))
        .removeData('href')
        .attr('data-toggle', 'tab')
        .html(title + $a.html().replace('resize-full', 'resize-small'))
        .removeClass('disabled');
      $t.trigger('tabbablechanged');
    });

    $t.on('click', '.tab-container .icon-minus-sign-alt', (e) => {
      e.stopPropagation();
      const $a = $(e.currentTarget).parent();
      $a.closest('.gadget').find('.tab-content').slideUp('fast');
      const title = $a.data('title');
      $(e.currentTarget).attr('title', `Show ${title}`);
      $a.data('href', $a.attr('href')).removeAttr('href').removeAttr('data-toggle').html($a.html().replace('minus-sign', 'plus-sign'));
      $t.trigger('tabbablechanged');
    });

    $t.on('click', '.tab-container .icon-plus-sign-alt', (e) => {
      e.stopPropagation();
      const $a = $(e.currentTarget).parent();
      $a.closest('.gadget').find('.tab-content').slideDown('fast');
      const title = $a.data('title');
      $(e.currentTarget).attr('title', `Hide ${title}`);
      $a.attr('href', $a.data('href')).removeData('href').attr('data-toggle', 'tab').html($a.html().replace('plus-sign', 'minus-sign'));
      $t.trigger('tabbablechanged');
    });

    // default view
    switch ($t.data('default-view')) {
      case 'tabbed':
        tabLayout();
        break;

      case 'list':
        listLayout();
        break;

      case 'gadget':
      default:
        gadgetLayout();
    }
  }

  // drag and drop containers
  $('.tabula-dnd').dragAndDrop();
  $('.tabula-filtered-list').filteredList();

  if (window !== window.top) {
    // this is an iframe
    (() => {
      let bodyHeight;
      try {
        bodyHeight = $('body').height();
      } catch (e) {
        // Hello, Firefox 59
        bodyHeight = document.body.clientHeight;
      }

      setInterval(() => {
        const newBodyHeight = $('body').height();
        if (newBodyHeight !== bodyHeight) {
          bodyHeight = newBodyHeight;
          window.parent.GlobalScripts.resizeModalIframes(newBodyHeight);
        }
      }, 500);
      window.parent.GlobalScripts.resizeModalIframes(bodyHeight);
    })();
  }

  // Prevent clicks on filtering dropdowns from closing window
  // and enable a close button
  $('.dropdown-menu.filter-list').on('click', (e) => {
    e.stopImmediatePropagation();
  }).find('button[data-dismiss=dropdown]').on('click', (e) => {
    e.stopPropagation();
    $(e.currentTarget).closest('.dropdown-menu').dropdown('toggle');
  });

  $('[data-loading-text]').on('click', (e) => {
    $(e.currentTarget).button('loading');
  });

  // SCRIPTS FOR ATTENDANCE NOTES
  (() => {
    const addArgToUrl = (url, argName, argValue) => {
      if (url.indexOf('?') > 0) {
        return `${url}&${argName}=${argValue}`;
      }
      return `${url}?${argName}=${argValue}`;
    };

    const attendanceNoteIframeLoad = (iFrame) => {
      const $m = $('#attendance-note-modal');
      const $f = $(iFrame).contents();

      if ($f.find('.attendance-note-success').length > 0) {
        // Save successful
        const linkId = $f.find('.attendance-note-success').data('linkid');
        const state = $f.find('.attendance-note-success').data('state');
        let $links;

        if (linkId === 'bulk') {
          $links = $('#recordAttendance').find('a.attendance-note');
        } else {
          $links = $(linkId);
        }

        $links.each((i, link) => {
          const $link = $(link);
          $link.attr('data-original-title', `${state} attendance note`);
          if (state === 'Edit') {
            $link.addClass('edit');
          } else {
            $link.removeClass('edit');
          }
        });

        $m.modal('hide');
      } else {
        $m.find('.modal-body').slideDown();
        const $form = $m.find('form.double-submit-protection');
        $form.tabulaSubmitOnce();
        const btn = $form.find('.btn').removeClass('disabled');
        if (btn.data('spinContainer')) {
          btn.data('spinContainer').spin(false);
        }
        // wipe any existing state information for the submit protection
        $form.removeData('submitOnceSubmitted');
        $m.modal('show');
        $m.on('shown.bs.modal', () => {
          $f.find('[name="note"]')
            .focus();
        });
      }
    };

    const attendanceNoteIframeHandler = function attendanceNoteIframeHandler() {
      attendanceNoteIframeLoad(this);
      $(this).off('load', attendanceNoteIframeHandler);
    };

    const attendanceNoteClickHandler = (href, $target) => {
      let $m = $('#attendance-note-modal');
      if ($m.length === 0) {
        $m = $('<div />').attr({
          id: 'attendance-note-modal',
          class: 'modal fade',
        }).appendTo($('body'));
      }

      $m.off('submit', 'form').on('submit', 'form', (e) => {
        e.preventDefault();
        // reattach the load handler and submit the inner form in the iframe
        $m.find('iframe')
          .on('load', attendanceNoteIframeHandler)
          .contents()
          .find('form')
          .submit();

        // hide the iframe, so we don't get a FOUC
        $m.find('.modal-body').slideUp();
        $m.find('form.double-submit-protection .spinnable').spin('small');
      });

      const $icon = $target.find('i');
      $icon.removeClass('fa-pencil-square-o').addClass('fa-spinner fa-spin');
      $.get(href, (data) => {
        $m.html(data);
        $m.find('.modal-body').empty();
        const iframeMarkup = '<iframe frameBorder="0" scrolling="no" style="height:100%;width:100%;" id="modal-content"></iframe>';
        $(iframeMarkup)
          .on('load', attendanceNoteIframeHandler)
          .attr('src', addArgToUrl(href, 'isIframe', 'true'))
          .appendTo($m.find('.modal-body'));
        $icon.removeClass('fa-spinner fa-spin').addClass('fa-pencil-square-o');
      });
    };

    $('.recordCheckpointForm .fix-area').on('click', 'a.btn.attendance-note', (event) => {
      event.preventDefault();
      attendanceNoteClickHandler($(event.currentTarget).attr('href'), $(event.currentTarget));
    });

    // Popovers are created on click so binding directly to A tags won't work
    $('body').on('click', '.popover a.attendance-note-modal', (event) => {
      const $this = $(event.currentTarget);
      let $m = $('#attendance-note-modal');
      event.preventDefault();
      if ($m.length === 0) {
        $m = $('<div />').attr({
          id: 'attendance-note-modal',
          class: 'modal fade',
        }).appendTo($('body'));
      }

      $.get($this.attr('href'), (data) => {
        $m.html(data).modal('show');
        $this.closest('.popover').find('button.close').trigger('click');
        $m.find('.modal-footer .btn-primary').on('click', (e) => {
          e.preventDefault();
          const link = $(e.currentTarget).attr('href');
          $m.modal('hide').on('hidden.bs.modal.attendance-note', () => {
            $m.off('hidden.bs.modal.attendance-note');
            attendanceNoteClickHandler(link, $());
          });
        });
      });
    });
  })();
  // END SCRIPTS FOR ATTENDANCE NOTES

  // Radio-style buttons
  $('[data-toggle="radio-buttons"]').on('click', 'button', (e) => {
    $(e.currentTarget).closest('[data-toggle="radio-buttons"]').find('button.active').removeClass('active');
    $(e.currentTarget).addClass('active');
  });

  // TAB-5314 when you click on an input-group-addon, focus that field
  $body.on('click', '.input-group-addon', (e) => {
    $(e.currentTarget).closest('.input-group')
      .find(':input:not(:focus):visible')
      .first()
      .focus();
  });
}); // on ready

// take anything we've attached to "exports" and add it to the global "Profiles"
// we use extend() to add to any existing variable rather than clobber it
window.GlobalScripts = jQuery.extend(window.GlobalScripts, exports);

$(() => {
// If there's an element with an id of 'scroll-container', max-size it to fit to the bottom of
// the page, with scrollbars if needed
  $('#scroll-container').each((i, el) => {
    const scrollable = $(el);
    // window.height includes horizontal scrollbar on mozilla so add 20px of padding.
    const elementHeight = ($(window).height() - scrollable.offset().top) - 20;
    scrollable.css({ 'max-height': elementHeight, 'overflow-y': 'auto' });
  });
});

// code for department settings - lives here as department settings is included in most modules
$(() => {
  const $deptSettingsForm = $('.department-settings-form');
  if (!$deptSettingsForm.length) return;

  $deptSettingsForm.find('input#plagiarismDetection').slideMoreOptions($('#turnitin-options'), true);

  $deptSettingsForm.find('input#turnitinExcludeSmallMatches').slideMoreOptions($('#small-match-options'), true);

  $deptSettingsForm.find('#small-match-options').on('tabula.slideMoreOptions.hidden', (e) => {
    // what is `this` here? can it ever be checked?
    if (!$(e.currentTarget).is(':checked')) {
      $('#small-match-options').find('input[type=text]').val('0');
    }
  }).find('input').on('disable.radiocontrolled', () => {
    this.value = '0';
  });

  $deptSettingsForm.find('input[name=disable-radio]').radioControlled({
    parentSelector: '.radio',
    selector: '.input-group',
    mode: 'readonly',
  });
});

$(() => {
  $(document.body).on('click', 'a.disabled', (e) => {
    e.preventDefault();
  });
  $(document.body).on('click', 'button.disabled', (e) => {
    e.preventDefault();
  });
});

$(() => {
  // be sure to bind the confirm-submit handler before other handlers on submit buttons
  $('a[data-toggle~="confirm-submit"][data-message], :button[data-toggle~="confirm-submit"][data-message], input[type="submit"][data-toggle~="confirm-submit"][data-message], input[type="button"][data-toggle~="confirm-submit"][data-message]').on('click', function confirmBeforeSubmit(event) {
    const $button = $(this);
    // eslint-disable-next-line no-alert
    if (!window.confirm($button.data('message'))) {
      event.preventDefault();
      event.stopImmediatePropagation();
    }
  });

  $.ajaxPrefilter((options, originalOptions, jqXHR) => {
    let safe = false;
    if (typeof URL === 'function' && (new URL(options.url, window.location.origin)).origin === window.location.origin) {
      safe = true;
    } else if (typeof URL !== 'function' && window.navigator.userAgent.indexOf('Trident/7.0') > -1) {
      const a = $('<a>', {
        href: options.url,
      });
      const linkHostname = a.prop('hostname');
      safe = (linkHostname === window.location.hostname || linkHostname === '');
    }

    if (safe) {
      const csrfHeaderName = $('meta[name=_csrf_header]').attr('content');
      const csrfHeaderValue = $('meta[name=_csrf]').attr('content');
      if (csrfHeaderName !== undefined && csrfHeaderValue !== undefined) {
        jqXHR.setRequestHeader(csrfHeaderName, csrfHeaderValue);
      }
    }
  });

  $('form[data-confirm-submit]').on('submit', (e) => {
    const $form = $(e.currentTarget);
    // eslint-disable-next-line no-alert
    if (!window.confirm($form.data('confirm-submit'))) {
      e.preventDefault();
      e.stopImmediatePropagation();
    }
  });
});

// TAB-7304 handle ajax error globally
$(() => {
  $(document).ajaxError((event, jqXhr, settings) => {
    // https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/readyState
    // we do not want to do anything if state is 0. e.g. user quickly navigated away.
    if (jqXhr.readyState === 0) return;
    const pageErrorToken = $('body').data('error-token');
    if (pageErrorToken) {
      const errorModal = $(`#global-error-modal-${pageErrorToken}`);
      if (errorModal) errorModal.modal('show');
    }

    // throw this so it could be handled by /error/js as we cannot assume failed ajax calls would
    // have reached backend (and logged)
    throw Error(`Ajax network error on ${window.location.href} when trying to ${settings.type} to ${settings.url}. error token: ${pageErrorToken || 'NA'}`);
  });
});

// TAB-7334 add tabindex to all modal
$(() => $('.modal').toArray().map($).forEach(modal => modal.attr('tabindex', -1)));
