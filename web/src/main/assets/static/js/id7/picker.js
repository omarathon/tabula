/* eslint-env browser */
/* eslint-disable class-methods-use-this */

import $ from 'jquery';
import log from 'loglevel';
// eslint-disable-next-line import/no-extraneous-dependencies
import 'bootstrap-3-typeahead';
import { escape } from 'lodash-es';
import RichResultField from './rich-result-field';

export default class Picker {
  constructor(input) {
    const self = this;
    const $element = $(input);
    this.$element = $element;

    $element.addClass('picker');
    // Disable browser autocomplete dropdowns, it gets in the way.
    $element.attr('autocomplete', 'off');

    this.richResultField = new RichResultField(input);
    this.currentQuery = null;

    $element.typeahead({
      source: (query, callback) => {
        this.doSearch(query, {}, callback);
      },
      delay: 200,
      matcher: () => true, // All data received from the server matches the query
      displayText: this.displayText.bind(this),
      highlighter: html => html,
      changeInputOnMove: false,
      afterSelect: (item) => {
        self.richResultField.store(this.valueFor(item), this.textFor(item));
        $element.data('item', item);
      },
      items: 20,
    });

    // On load, look up the existing value and give it human-friendly text if possible
    // NOTE: this relies on the fact that the saved value is itself a valid search term
    const currentValue = $element.val();
    if (currentValue && currentValue.trim().length > 0) {
      this.doSearch(currentValue, { exact: true }, (results) => {
        if (results.length > 0) {
          self.richResultField.storeText(this.textFor(results[0]));
        }
      });
    }
  }

  doSearch(query, options, callback) {
    this.currentQuery = query;

    this.makeRequest(query, options)
      .then(response => response.json())
      .catch((e) => {
        log.error(e);
        return [];
      })
      .then((response) => {
        // Return the items only if the user hasn't since made a different query
        if (this.currentQuery === query) {
          callback(response && response.data && response.data.results ? response.data.results : []);
        }
      });
  }

  displayText(item) {
    return `<div class="flexi-picker-result">
      ${escape(this.textFor(item))}
    </div>`;
  }

  valueFor() {
    throw new Error('Not implemented');
  }

  textFor() {
    throw new Error('Not implemented');
  }

  makeRequest() {
    throw new Error('Not implemented');
  }
}
