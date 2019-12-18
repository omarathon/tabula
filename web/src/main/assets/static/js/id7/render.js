/* eslint-env browser */
require('@universityofwarwick/id7/dist/js/id7-bundle');
require('@universityofwarwick/statuspage-widget/dist/main');

import moment from 'moment-timezone'; // eslint-disable-line import/first,import/newline-after-import
window.moment = moment;

// TODO do we need all of these?
require('jquery-ui/ui/core');
require('jquery-ui/ui/widget');
require('jquery-ui/ui/widgets/mouse');
require('jquery-ui/ui/widgets/draggable');
require('jquery-ui/ui/widgets/droppable');
require('jquery-ui/ui/widgets/resizable');
require('jquery-ui/ui/widgets/selectable');
require('jquery-ui/ui/widgets/sortable');

require('jquery-caret-plugin/dist/jquery.caret');
require('fixed-header-table/jquery.fixedheadertable');
require('../../libs/jquery-tablesorter/jquery.tablesorter'); // TODO rewrite code to be compatible with npm-published version
require('../../libs/bootstrap-datetimepicker/js/bootstrap-datetimepicker'); // TODO this is a patched version I can't find in npm
require('../../libs/bootstrap3-editable/js/bootstrap-editable'); // TODO bizarrely npm only has 1.5.1 and we use 1.5.3, we also patch for fa icons

window.Spinner = require('spin.js/spin').Spinner;

const $ = window.jQuery;

$.fn.spin = function spin(o, color) {
  return this.each((i, el) => {
    const $this = $(el);
    const data = $this.data();

    if (data.spinner) {
      data.spinner.stop();
      delete data.spinner;
    }

    if (o !== false) {
      const opts = $.extend(
        { color: color || $this.css('color') },
        $.fn.spin.presets[o] || o,
      );

      data.spinner = new window.Spinner(opts).spin(el);
    }
  });
};

$.fn.spin.presets = {
  tiny: {
    lines: 8,
    length: 2,
    width: 2,
    radius: 3,
  },
  small: {
    lines: 8,
    length: 4,
    width: 3,
    radius: 5,
  },
  large: {
    lines: 10,
    length: 8,
    width: 4,
    radius: 8,
  },
};

require('../jquery-copyable');
require('../jquery-details');
require('../jquery.are-you-sure');
require('./jquery-draganddrop');
require('../jquery-filteredlist');
require('../jquery-collapsible');
require('./jquery-fixheaderfooter');
require('../jquery-scrolltofixed');
require('../jquery-radiocontrolled');
require('./flexipicker');
require('../ajax-popup');
require('../combo-typeahead');
require('./map-popups');
require('../jquery-biglist');
require('./jquery-expandingTable');
require('../jquery.form');
require('./jquery-tableform');
require('../select-deselect-checkboxes');
