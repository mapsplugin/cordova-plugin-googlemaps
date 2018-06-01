//
//  MyPluginScrollView.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <UIKit/UIKit.h>

@interface MyPluginScrollView : UIScrollView

@property (nonatomic) NSMutableDictionary *mapCtrls;
@property (nonatomic) NSMutableDictionary *HTMLNodes;
- (void)attachView:(UIView *)view depth:(NSInteger)depth;
- (void)detachView:(UIView *)view;
@end
