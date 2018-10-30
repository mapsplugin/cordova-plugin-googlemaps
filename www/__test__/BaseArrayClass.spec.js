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
      expect(_.mapSeries((item, next) => {
        setTimeout(() => {
          next(item);
        }, Math.random(3));
      })).resolves.toEqual(initArray);
    });
  });

  describe('mapAsync()', () => {
    it('should keep item order', () => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      expect(_.mapAsync((item, next) => {
        setTimeout(() => {
          next(item);
        }, Math.random(3));
      })).resolves.toEqual(initArray);
    });
  });

  describe('map()', () => {
    it('should work as `mapAsync()` if callback is given', (done) => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      _.map((item, next) => {
        setTimeout(() => {
          next(item);
        }, Math.random(3));
      }, (results) => {
        expect(results).toEqual(initArray);
        done();
      });
    });

    it('should work as `Array.map()` if callback is omitted', () => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      expect(_.map((item) => {
        return item;
      })).toEqual(initArray);
    });
  });

  describe('forEachAsync()', () => {
    it('should execute `iteratee` 3 times', (done) => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      let i = 0;
      _.forEachAsync((item, next) => {
        setTimeout(() => {
          i++;
          next(item);
        }, 0);
      }).then(() => {
        expect(i).toBe(3);
        done();
      });
    });
  });

  describe('forEach()', () => {
    it('should work as `forEach()` if callback is given', (done) => {
    let i = 0;
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      _.forEach((item, next) => {
        setTimeout(() => {
          i++;
          next();
        }, Math.random(3));
      }, () => {
        expect(i).toBe(3);
        done();
      });
    });

    it('should work as `Array.forEach()` if callback is omitted', () => {
      let i = 0;
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      _.forEach((item) => {
        i++
      })
      expect(i).toBe(3);
    });
  });

  describe('filterAsync()', () => {
    it('should return [0, 2, 4]', (done) => {
      const initArray = [0, 1, 2, 3, 4, 5];
      const _ = new BaseArrayClass(initArray);
      _.filterAsync((item, next) => {
        setTimeout(() => {
          next(item % 2 === 0);
        }, Math.random(5));
      }).then((results) => {
        expect(results).toEqual([0, 2, 4]);
        done();
      });
    });
  });

  describe('filter()', () => {
    it('should work as `filter()` if callback is given', (done) => {
    let i = 0;
      const initArray = [0, 1, 2, 3, 4, 5];
      const _ = new BaseArrayClass(initArray);
      _.filter((item, next) => {
        setTimeout(() => {
          next(item % 2 !== 0);
        }, Math.random(3));
      }, (results) => {
        expect(results).toEqual([1, 3, 5]);
        done();
      });
    });

    it('should work as `Array.filter()` if callback is omitted', () => {
      let i = 0;
      const initArray = [
        {condition: true},
        {condition: false},
        {condition: true}
      ];
      const _ = new BaseArrayClass(initArray);
      expect(_.filter((item) => {
        return item.condition === true
      })).toHaveLength(2);
    });
  });

  describe('indexOf()', () => {
    it('should find the first element position', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      expect(_.indexOf('World')).toBe(1);
    });

    it('should find the second element position', () => {
      const initArray = ['Hello', 'World', 'test', 'World'];
      const _ = new BaseArrayClass(initArray);
      expect(_.indexOf('World', 2)).toBe(3);
    });
  });

  describe('empty()', () => {
    it('should delete all elements', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      _.empty();
      expect(_.getLength()).toBe(0);
    });

  });

  describe('push()', () => {
    it('should add the item to the last', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      _.push('HelloWorld');
      expect(_.getAt(3)).toEqual('HelloWorld');
    });
  });

  describe('insertAt()', () => {
    it('should add the item at the specified position', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      _.insertAt(1, 'Aloha');
      expect(_.getArray()).toEqual(['Hello', 'Aloha', 'World', 'test']);
    });
  });


  describe('getArray()', () => {
    it('should return the same array', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      expect(_.getArray()).toEqual(['Hello', 'World', 'test']);
    });
  });

  describe('getAt()', () => {
    it('should return the same item at specified position', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      expect(_.getAt(1)).toEqual('World');
    });
  });

  describe('setAt()', () => {
    it('should replace the item of specified position', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      _.setAt(0, 'Aloha');
      expect(_.getAt(0)).toEqual('Aloha');
    });
  });

  describe('removeAt()', () => {
    it('should remove the item of specified position', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      _.removeAt(1);
      expect(_.getAt(1)).toEqual('test');
      expect(_.getLength()).toBe(2);
    });
  });

  describe('pop()', () => {
    it('should return the same item of the last item', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      expect(_.pop()).toEqual('test');
      expect(_.getLength()).toBe(2);
    });
  });


  describe('reverse()', () => {
    it('should reverse the array', () => {
      const initArray = ['Hello', 'World', 'test'];
      const _ = new BaseArrayClass(initArray);
      _.reverse();
      expect(_.getArray()).toEqual(['test', 'World', 'Hello']);
    });
  });

  describe('sort()', () => {
    it('should return ["A", "B", "C", "a", "b", "c"]', () => {
      const initArray = ['c', 'C', 'A', 'B', 'b', 'a'];
      const _ = new BaseArrayClass(initArray);
      _.sort();
      expect(_.getArray()).toEqual(['A', 'B', 'C', 'a', 'b', 'c']);
    });

    it('should return ["a", "A", "b", "B", "c", "C"]', () => {
      const initArray = ['c', 'C', 'A', 'B', 'b', 'a'];
      const _ = new BaseArrayClass(initArray);
      _.sort((a, b) => {
        let charCodeA = a.charCodeAt(0);
        let charCodeB = b.charCodeAt(0);
        let lowerA = charCodeA > 96 ? charCodeA - 32 : charCodeA;
        let lowerB = charCodeB > 96 ? charCodeB - 32 : charCodeB;
        return lowerA === lowerB ? charCodeB - charCodeA: lowerA - lowerB;
      });
      expect(_.getArray()).toEqual(['a', 'A', 'b', 'B', 'c', 'C']);
    });
  });
});
