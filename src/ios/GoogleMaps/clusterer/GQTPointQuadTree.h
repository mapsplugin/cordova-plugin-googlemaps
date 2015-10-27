#import <Foundation/Foundation.h>
#import "GQTPointQuadTreeItem.h"
#import "GQTBounds.h"

@interface GQTPointQuadTree : NSObject


/**
 * Create a QuadTree with bounds. Please note, this class is not thread safe.
 *
 * @param bounds The bounds of this PointQuadTree. The tree will only accept items that fall
                 within the bounds. The bounds are inclusive.
 */
- (id)initWithBounds:(GQTBounds)bounds;

/**
 * Create a QuadTree with the inclusive bounds of (-1,-1) to (1,1).
 */
- (id)init;

/**
 * Insert an item into this PointQuadTree.
 *
 * @param item The item to insert. Must not be nil.
 * @return |NO| if the item is not contained within the bounds of this tree.
 *         Otherwise adds the item and returns |YES|.
 */
- (BOOL)add:(id<GQTPointQuadTreeItem>)item;

/**
 * Delete an item from this PointQuadTree.
 *
 * @param item The item to delete.
 * @return |NO| if the items was not found in the tree, |YES| otherwise.
 */
- (BOOL)remove:(id<GQTPointQuadTreeItem>)item;

/**
 * Delete all items from this PointQuadTree.
 */
- (void)clear;

/**
 * Retreive all items in this PointQuadTree within a bounding box.
 *
 * @param bounds The bounds of the search box.
 * @return The collection of items within |bounds|, returned as an NSArray
 *         of id<GQTPointQuadTreeItem>.
 */
- (NSArray *)searchWithBounds:(GQTBounds)bounds;

/**
 * The number of items in this entire tree.
 *
 * @return The number of items.
 */
- (NSUInteger)count;

@end
