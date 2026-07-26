//
//  AFServiceCenter.h
//  AFServiceSDK
//
//  Copyright © 2018 Ant Group. All rights reserved.
//

#import <Foundation/Foundation.h>

@class AFAuthServiceResponse;

/// 极简授权 SDK 支持的业务枚举值
typedef NS_ENUM(NSUInteger, AFService) {
    AFServiceEInvoice,  //!< 电子发票
    AFServiceAuth,      //!< 三方授权
    AFServiceDeduct     //!< 代扣
};

extern NSString * const kAFServiceOptionBizParams;      // 授权服务调用入参
extern NSString * const kAFServiceOptionCallbackScheme; // 授权业务回跳当前 app 的 scheme（重要）
extern NSString * const kAFServiceOptionNotUseLanding;  // 不使用提示下载页做补偿，为 true 时需要商户自己处理用户未安装的情况
extern NSString * const kAFServiceBizParamsKeyUrl;      // 独立签约入参 url

typedef void(^AFAuthServiceResultBlock)(AFAuthServiceResponse *response);

@interface AFServiceCenter : NSObject

/// 调用授权服务入口
/// @param service 业务 service 类型, 见 AFService 枚举值
/// @param params 参数字典，key 值详情参见 kAFServiceOptionBizParams、kAFServiceOptionCallbackScheme 的注释
/// @param block 业务结果回调, block 参数是 AFAuthServiceResponse 类型，业务结果通过 result 属性获取。如果未用户未安装并且 kAFServiceOptionNotUseLanding 未设置为 true，会使用 H5 Landing 页做补偿，这种情况下不会有 block 回调结果。
+ (void)callService:(AFService)service
         withParams:(NSDictionary *)params
      andCompletion:(AFAuthServiceResultBlock)block;

/// 处理授权服务回跳接入方 app 的 URL
/// @param url 回跳 URL
/// @param block 业务结果回调。注意此接口上的 block 只有在跳转授权后，当前 app 被系统回收的情况下回跳才生效。
+ (void)handleResponseURL:(NSURL *)url
           withCompletion:(AFAuthServiceResultBlock)block;

/// 判断当前是否支持标准授权
+ (BOOL)canOpenAuth;

@end
