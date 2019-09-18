/* eslint-env browser */
/* eslint-disable class-methods-use-this */

import $ from 'jquery';
import { getJsonWithCredentials } from '@universityofwarwick/serverpipe';
import Picker from './picker';

export default class ProfilePicker extends Picker {
  constructor(input) {
    super(input);
    this.$element.addClass('profile-picker');
  }

  makeRequest(query) {
    return getJsonWithCredentials(`/profiles/relationships/agents/search.json?query=${encodeURIComponent(query)}`);
  }

  textFor(item) {
    console.log(item);
    if (item.inuse) return item.name;
    return `${item.name} (inactive)`;
  }

  valueFor(item) {
    console.log(item);
    return {
      title: item.name,
      description: `${item.userId} ${item.description}`,
      value: item.userId,
    };
  }
}

// The jQuery plugin
$.fn.profilePicker = function profilePicker() {
  return this.each((i, element) => {
    const $this = $(element);
    if (!$this.data('profile-picker')) {
      $this.data('profile-picker', new ProfilePicker(element));
    }
  });
};

$(() => {
  $('.profile-picker').profilePicker();
});
