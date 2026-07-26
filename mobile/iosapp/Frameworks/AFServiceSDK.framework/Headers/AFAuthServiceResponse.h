//
//  AFAuthServiceResponse.h
//  AFServiceSDK
//
//  Copyright © 2018 Ant Group. All rights reserved.
//

#import <Foundation/Foundation.h>

/// 授权服务调用结果状态码
typedef NS_ENUM(NSUInteger, AFAuthResCode) {
    AFAuthResSuccess = 0, //!< 默认值，业务调用成功，结果数据参见 result 字段
    AFAuthResInvalidService = 100, //!< 调用服务 Service 枚举值错误
    AFAuthResInvalidURL, //!< 授权回跳 URL 错误
    AFAuthResRepeatCall, //!< 业务重复调用（3s内）
    AFAuthResOpenURLErr, //!< 跳转授权失败
};

@interface AFAuthServiceResponse : NSObject

/// 业务调用状态码
@property (nonatomic, assign) AFAuthResCode responseCode;

/// 业务结果 Dictionary, 内容请参考具体业务方接入文档
@property (readonly) NSDictionary *result;

@end
