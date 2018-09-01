/*jshint esversion: 6 */

const values = require('../values');

class InfoPlistNotFoundError extends Error {
  constructor(manifestPath) {
    super(`There is no ${manifestPath}.`);
  }

  getSolution(projectPlatform) {
    let definedMessage = [];

    switch(projectPlatform) {
      case values.PLATFORM_CAPACITOR:
        definedMessage.push("  Please execute '$> npx cap sync' before this command.");
        break;

      case values.PLATFORM_CORDOVA:
      case values.PLATFORM_CORDOVA_OLD:
        definedMessage.push("  Please reinstall the maps plugin:");
        definedMessage.push("    $> cordova platform rm ios");
        definedMessage.push("    $> cordova platform add ios");
        break;
    }
    return definedMessage.join("\n");
  }
}

module.exports = AndroidManifestNotFoundError;
