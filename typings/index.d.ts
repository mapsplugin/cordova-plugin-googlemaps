interface ILatLng {
  lat: number;
  lng: number;
}

interface ILatLngBounds {
  northeast: ILatLng;
  southwest: ILatLng;
}

interface GeocoderRequest {
  /**
   * The address property or position property is required.
   * You can not specify both property at the same time.
   *
   * [geocoding usage1]
   * let request: GeocoderRequest = {
   *   address: "Los Angeles, California, USA"
   * }
   *
   * [geocoding usage2]
   * let request: GeocoderRequest = {
   *   address: [
   *    "Los Angeles, California, USA",
   *    "San Francisco, California, USA",
   *   ]
   * }
   */
  address?: string | string[];
  /**
   *
   * [reverse-geocoding usage1]
   * let request: GeocoderRequest = {
   *   position: {"lat": 37.421655, "lng": -122.085637}
   * }
   *
   * [reverse-geocoding usage2]
   * let request: GeocoderRequest = {
   *   position: [
   *    {"lat": 37.421655, "lng": -122.085637},
   *    {"lat": 37.332, "lng": -122.030781}
   *   ]
   * }
   */
  position?: ILatLng | ILatLng[];

  bounds?: ILatLng | ILatLng[];
}

interface GeocoderResult {
  adminArea?: string;
  country?: string;
  countryCode?: string;
  extra?: {
    featureName?: string;
    lines?: string[];
    permises?: string;
    phone?: string;
    url?: string;
  };
  locale?: string;
  locality?: string;
  position?: ILatLng;
  postalCode?: string;
  subAdminArea?: string;
  subLocality?: string;
  subThoroughfare?: string;
  thoroughfare?: string;
}
