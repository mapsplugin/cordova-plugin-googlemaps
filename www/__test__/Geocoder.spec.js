// jshint esversion: 6
const Geocoder = require('../Geocoder');

describe('Geocode', () => {
  // Initialize them here so that vscode typing
  // will work without this being a TS project
  let geocoder = new Geocoder();
  let execSpy = jest.fn();

  beforeEach(() => {
    execSpy = jest.fn();
    geocoder = new Geocoder(execSpy);
  });

  it('should exist', () => {
    expect(geocoder).toBeDefined();
  });

  describe('geocode()', () => {
    it('should exist', () => {
      expect(geocoder.geocode).toBeDefined();
    });

    fit('should return a promise if a callback is not passed', async () => {
      const request = {
        address: '',
      };
      const mockResults = [
        {
          foo: 'bar',
        },
      ];
      execSpy.mockImplementation(resolve => {
        resolve({
          results: mockResults,
        });
      });

      const results = geocoder.geocode(request);
      expect(results).toMatchSnapshot();
      await expect(results).resolves.toMatchSnapshot();
    });

    it('should error if both position and address options are provided', async () => {
      const request = {
        position: [],
        address: [],
      };

      await expect(geocoder.geocode(request)).rejects.toMatchSnapshot();
    });

    it('should call the if both position and address options are provided', () => {
      const request = {
        position: [],
        address: [],
      };

      expect(geocoder.geocode(request)).rejects.toMatchSnapshot();
    });

    describe('single request', () => {
      describe('position', () => {
        it('should call the native plugin with the correct parameters', () => {
          geocoder.geocode({
            position: {
              lat: 0,
              lng: 0,
            },
          });
          expect(execSpy.mock.calls).toMatchSnapshot();
        });
      });

      describe('address', () => {
        it('should call the native plugin with the correct parameters', () => {
          geocoder.geocode({
            address: 'Tokyo, Japan',
          });
          expect(execSpy.mock.calls).toMatchSnapshot();
        });
      });
    });

    xdescribe('multiple requests', () => {
      // todo
    });
  });
});
