/*jshint esversion: 6 */

const values = require('../values');
const Styles = require('shell/lib/Styles');

class ConfigXmlNotFoundError extends Error {
  constructor(configXmlPath) {
    super(`There is no ${configXmlPath}.`);
  }

}

module.exports = ConfigXmlNotFoundError;
