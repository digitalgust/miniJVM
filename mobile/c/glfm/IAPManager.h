//
//  IAPManager.h
//  iosapp
//
//  Created by Gust on 2024/9/8.
//  Copyright © 2024 Gust. All rights reserved.
//

#ifndef IAPManager_h
#define IAPManager_h

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef enum {
    IAPPurchSuccess = 0,       // 购买成功
    IAPPurchFailed = 1,        // 购买失败
    IAPPurchCancel = 2,        // 取消购买
    IAPPurchVerFailed = 3,     // 订单校验失败
    IAPPurchVerSuccess = 4,    // 订单校验成功
    IAPPurchNotArrow = 5,      // 不允许内购
}IAPPurchType;

typedef void (^IAPCompletionHandle)(IAPPurchType type,NSData *data);



@interface IAPManager : NSObject
+ (instancetype)shareIAPManager;
- (void)startPurchaseWithID:(NSString *)purchID completeHandle:(IAPCompletionHandle)handle;
@end

NS_ASSUME_NONNULL_END

#endif /* IAPManager_h */
