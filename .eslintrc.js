module.exports = {
  "env": {
    "browser": true,
    "node": true
  },
  "extends": "eslint:recommended",
  "parserOptions": {
    "ecmaVersion": 5
  },
  "rules": {
    "indent": [
      "error",
      2
    ],
    "linebreak-style": [
      "error",
      "unix"
    ],
    "quotes": [
      "error",
      "single"
    ],
    "semi": [
      "error",
      "always"
    ],
    "no-console": 0
  },
  "globals": {
    "require": false,
    "Symbol": false,
    "window": false,
    "cordova": false,
    "Promise": false,
    "module": false,
    "angular": false,
    "firebase": false,
    "exports": false,
    "google": false,
    "plugin": false
  }
};
