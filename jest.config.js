module.exports = {
  moduleNameMapper: {
    '^cordova/(.*)$': '<rootDir>/node_modules/cordova-js/src/common/$1',
  },
  globals: {
    cordova: {},
  },
};
