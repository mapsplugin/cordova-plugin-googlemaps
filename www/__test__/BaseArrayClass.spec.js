// jshint esversion: 6
const BaseArrayClass = require('../BaseArrayClass');

describe('[BaseArrayClass]', () => {

  describe('getLength()', () => {
    it('() should return the same size with initial array', () => {
      const initArray = [0, 1, 2];
      const _ = new BaseArrayClass(initArray);
      expect(_.getLength()).toBe(initArray.length);
    });
  });


  describe('mapSeries()', () => {
    it('should keep item order', () => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      expect(_.mapSeries(function(item, next) {
        next(item);
      })).resolves.toEqual(initArray);
    });
  });

  describe('mapAsync()', () => {
    it('should keep item order', () => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      expect(_.mapAsync(function(item, next) {
        next(item);
      })).resolves.toEqual(initArray);
    });
  });

  describe('map()', () => {
    it('should work as `mapAsync()` if callback is given', (done) => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      _.map(function(item, next) {
        next(item);
      }, function(results) {
        expect(results).toEqual(initArray);
        done();
      });
    });

    it('should work as `Array.map()` if callback is omitted', () => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      expect(_.map(function(item) {
        return item;
      })).toEqual(initArray);
    });
  });
});
