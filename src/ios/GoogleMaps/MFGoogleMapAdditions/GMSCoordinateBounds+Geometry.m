//
//  GMSCoordinateBounds+Geometry.m
//
//  Created by Marius Feldmann on 8/12/14.
//  Copyright (c) 2014 Marius Feldmann. All rights reserved.
//

#import "GMSCoordinateBounds+Geometry.h"

#define RADIANS_TO_DEGREES(radians) ((radians) * (180.0 / M_PI))

@implementation GMSCoordinateBounds (MFAdditions)

- (CLLocationCoordinate2D)southEast {
	CLLocationDegrees southEastLat = self.southWest.latitude;
	CLLocationDegrees southEastLng = self.northEast.longitude;
	CLLocationCoordinate2D southEast = CLLocationCoordinate2DMake(southEastLat, southEastLng);
	
	return southEast;
}


- (CLLocationCoordinate2D)northWest {
	CLLocationDegrees northWestLat = self.northEast.latitude;
	CLLocationDegrees northWestLng = self.southWest.longitude;
	CLLocationCoordinate2D northWest = CLLocationCoordinate2DMake(northWestLat, northWestLng);
	
	return northWest;
}


- (CLLocationCoordinate2D)center {
	double fraction = 0.5;
	CLLocationCoordinate2D center = GMSGeometryInterpolate(self.northEast, self.southWest, fraction);
	
	return center;
}


- (GMSPath *)path {
	GMSMutablePath *path = [[GMSMutablePath alloc] init];
	[path addCoordinate:self.northWest];
	[path addCoordinate:self.northEast];
	[path addCoordinate:self.southEast];
	[path addCoordinate:self.southWest];
	
	return path;
}


- (NSArray *)divideIntoNumberOfBoxes:(NSInteger)numberOfBoxes {
	NSMutableArray *rectArray = [[NSMutableArray alloc] init];
	
	NSInteger columns = ceil(sqrt(numberOfBoxes));
	NSInteger fullRows = numberOfBoxes / columns;
	NSInteger orphans = numberOfBoxes % columns;
	
	double width = GMSGeometryDistance(self.northWest, self.northEast);
	double boxWidth = width / columns;
	
	double height = GMSGeometryDistance(self.northEast, self.southEast);
	double boxHeight = height / (orphans == 0 ? fullRows : (fullRows+1));
	
	for (int y=0; y<fullRows; y++) {
		for (int x=0; x<columns; x++) {
			// Calculate the diagonal of the rect
			double diagonal = sqrt(pow(boxWidth, 2) + pow(boxHeight, 2));
			
			// Get the angle of the diagonal
			double angle = 180 + RADIANS_TO_DEGREES(atan2(boxWidth, boxHeight));
			
			// Get the northEast Coord by moving the northEast Coord to the right
			CLLocationCoordinate2D northEastCoord = GMSGeometryOffset(self.northWest, boxWidth*(x+1), 90);

			// Make sure we are in the right row
			northEastCoord = GMSGeometryOffset(northEastCoord, boxHeight*y, 180);
			
			// Get the southWest Coord by following the diagonal line from the northEast Coord
			CLLocationCoordinate2D southWestCoord = GMSGeometryOffset(northEastCoord, diagonal, angle);
			
			// Create the new rect
			GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithCoordinate:southWestCoord coordinate:northEastCoord];
			[rectArray addObject:bounds];
		}
	}
	
	if (orphans > 0) {
		NSInteger orphanWidth = width / orphans;
		
		for (int x=0; x<orphans; x++) {
			double diagonal = sqrt(pow(orphanWidth, 2) + pow(boxHeight, 2));
			double angle = 180 + RADIANS_TO_DEGREES(atan2(orphanWidth, boxHeight));
			
			CLLocationCoordinate2D northEastCoord = GMSGeometryOffset(self.northWest, orphanWidth*(x+1), 90);
			northEastCoord = GMSGeometryOffset(northEastCoord, boxHeight*fullRows+1, 180);
			CLLocationCoordinate2D southWestCoord = GMSGeometryOffset(northEastCoord, diagonal, angle);
			
			GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithCoordinate:southWestCoord coordinate:northEastCoord];
			[rectArray addObject:bounds];
		}
	}
	
	return rectArray;
}

@end
