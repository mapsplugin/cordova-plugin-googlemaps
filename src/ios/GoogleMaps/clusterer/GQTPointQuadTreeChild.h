#import <Foundation/Foundation.h>
#import "GQTPointQuadTreeItem.h"
#import "GQTBounds.h"

/**
 * This is an internal class, use |GQTPointQuadTree| instead.
 * Please note, this class is not thread safe.
 *
 * This class represents an internal node of a |GQTPointQuadTree|.
 */

@interface GQTPointQuadTreeChild : NSObject

/**
 * Insert an item into this PointQuadTreeChild
 *
 * @param item The item to insert. Must not be nil.
 * @param bounds The bounds of this node.
 * @param depth The depth of this node.
 */
-    (void)add:(id<GQTPointQuadTreeItem>)item
 withOwnBounds:(GQTBounds)bounds
       atDepth:(NSUInteger)depth;

/**
 * Delete an item from this PointQuadTree.
 *
 * @param item The item to delete.
 * @param bounds The bounds of this node.
 * @return |NO| if the items was not found in the tree, |YES| otherwise.
 */
- (BOOL)remove:(id<GQTPointQuadTreeItem>)item withOwnBounds:(GQTBounds)bounds;

/**
 * Retreive all items in this PointQuadTree within a bounding box.
 *
 * @param searchBounds The bounds of the search box.
 * @param ownBounds    The bounds of this node.
 * @param accumulator  The results of the search.
 */
- (void)searchWithBounds:(GQTBounds)searchBounds
           withOwnBounds:(GQTBounds)ownBounds
                 results:(NSMutableArray *)accumulator;

/**
 * Split the contents of this Quad over four child quads.
 * @param ownBounds    The bounds of this node.
 * @param accumulator  The results of the search.
 */
- (void)splitWithOwnBounds:(GQTBounds)ownBounds atDepth:(NSUInteger)depth;

@end
