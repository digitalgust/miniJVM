//
//  IAPManager.m
//  iosapp
//
//  Created by Gust on 2024/9/8.
//  Copyright © 2024 Gust. All rights reserved.
//

#import <Foundation/Foundation.h>


#import "IAPManager.h"
#import <Foundation/Foundation.h>
#import <StoreKit/StoreKit.h>

@interface IAPManager()<SKPaymentTransactionObserver,SKProductsRequestDelegate>{
   NSString           *_currentPurchasedID;
   IAPCompletionHandle _iAPCompletionHandle;
}
@end

@implementation IAPManager
 
+ (instancetype)shareIAPManager{
     
    static IAPManager *iAPManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken,^{
        iAPManager = [[IAPManager alloc] init];
    });
    return iAPManager;
}
- (instancetype)init{
    self = [super init];
    if (self) {
        [[SKPaymentQueue defaultQueue] addTransactionObserver:self];
    }
    return self;
}
 
- (void)dealloc{
    [[SKPaymentQueue defaultQueue] removeTransactionObserver:self];
}
 
 
- (void)startPurchaseWithID:(NSString *)purchID completeHandle:(IAPCompletionHandle)handle{
    if (purchID) {
        if ([SKPaymentQueue canMakePayments]) {
            _currentPurchasedID = purchID;
            _iAPCompletionHandle = handle;
            
            //从App Store中检索关于指定产品列表的本地化信息
            NSSet *nsset = [NSSet setWithArray:@[purchID]];
            SKProductsRequest *request = [[SKProductsRequest alloc] initWithProductIdentifiers:nsset];
            request.delegate = self;
            [request start];
        }else{
            [self handleActionWithType:IAPPurchNotArrow data:nil];
        }
    }
}

- (void)handleActionWithType:(IAPPurchType)type data:(NSData *)data{
#if DEBUG
    switch (type) {
        case IAPPurchSuccess:
            NSLog(@"购买成功");
            break;
        case IAPPurchFailed:
            NSLog(@"购买失败");
            break;
        case IAPPurchCancel:
            NSLog(@"用户取消购买");
            break;
        case IAPPurchVerFailed:
            NSLog(@"订单校验失败");
            break;
        case IAPPurchVerSuccess:
            NSLog(@"订单校验成功");
            break;
        case IAPPurchNotArrow:
            NSLog(@"不允许程序内付费");
            break;
        default:
            break;
    }
#endif
    if(_iAPCompletionHandle){
        _iAPCompletionHandle(type,data);
    }
}
 
- (void)verifyPurchaseWithPaymentTransaction:(SKPaymentTransaction *)transaction{
    //交易验证
    NSURL *recepitURL = [[NSBundle mainBundle] appStoreReceiptURL];
    NSData *receipt = [NSData dataWithContentsOfURL:recepitURL];
     
    if(!receipt){
        // 交易凭证为空验证失败
        [self handleActionWithType:IAPPurchVerFailed data:nil];
        return;
    }
    // 购买成功将交易凭证发送给服务端进行再次校验
    [self handleActionWithType:IAPPurchSuccess data:receipt];
    
    // 验证成功与否都注销交易,否则会出现虚假凭证信息一直验证不通过,每次进程序都得输入苹果账号
    [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
}
 
#pragma mark - SKProductsRequestDelegate
- (void)productsRequest:(SKProductsRequest *)request didReceiveResponse:(SKProductsResponse *)response{
    NSArray *product = response.products;
    if([product count] <= 0){
#if DEBUG
        NSLog(@"--------------没有商品------------------");
#endif
        return;
    }
     
    SKProduct *p = nil;
    for(SKProduct *pro in product){
        if([pro.productIdentifier isEqualToString:_currentPurchasedID]){
            p = pro;
            break;
        }
    }
     
#if DEBUG
    NSLog(@"productID:%@", response.invalidProductIdentifiers);
    NSLog(@"产品付费数量:%lu",(unsigned long)[product count]);
    NSLog(@"产品描述:%@",[p description]);
    NSLog(@"产品标题%@",[p localizedTitle]);
    NSLog(@"产品本地化描述%@",[p localizedDescription]);
    NSLog(@"产品价格：%@",[p price]);
    NSLog(@"产品productIdentifier：%@",[p productIdentifier]);
#endif
     
    SKPayment *payment = [SKPayment paymentWithProduct:p];
    [[SKPaymentQueue defaultQueue] addPayment:payment];
}
 
//请求失败
- (void)request:(SKRequest *)request didFailWithError:(NSError *)error{
#if DEBUG
    NSLog(@"------------------从App Store中检索关于指定产品列表的本地化信息错误-----------------:%@", error);
#endif
}
 
- (void)requestDidFinish:(SKRequest *)request{
#if DEBUG
    NSLog(@"------------requestDidFinish-----------------");
#endif
}
 
#pragma mark - SKPaymentTransactionObserver
- (void)paymentQueue:(SKPaymentQueue *)queue updatedTransactions:(NSArray<SKPaymentTransaction *> *)transactions{
    for (SKPaymentTransaction *tran in transactions) {
        switch (tran.transactionState) {
            case SKPaymentTransactionStatePurchased:
                [self verifyPurchaseWithPaymentTransaction:tran];
                break;
            case SKPaymentTransactionStatePurchasing:
#if DEBUG
                NSLog(@"商品添加进列表");
#endif
                break;
            case SKPaymentTransactionStateRestored:
#if DEBUG
                NSLog(@"已经购买过商品");
#endif
                // 消耗型不支持恢复购买
                [[SKPaymentQueue defaultQueue] finishTransaction:tran];
                break;
            case SKPaymentTransactionStateFailed:
                [self failedTransaction:tran];
                break;
            default:
                break;
        }
    }
}

// 交易失败
- (void)failedTransaction:(SKPaymentTransaction *)transaction{
    if (transaction.error.code != SKErrorPaymentCancelled) {
        [self handleActionWithType:IAPPurchFailed data:nil];
    }else{
        [self handleActionWithType:IAPPurchCancel data:nil];
    }
     
    [[SKPaymentQueue defaultQueue] finishTransaction:transaction];
}
@end


// 声明全局函数
/*
 -(void)purchaseWithProductID:(NSString *)productID{
      
     [[IAPManager shareIAPManager] startPurchaseWithID:productID completeHandle:^(IAPPurchType type,NSData *data) {
          
     }];
 }
*/



