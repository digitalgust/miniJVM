# 支付宝极简版登录 SDK

`AFServiceSDK.framework` 来自支付宝开放平台“极简版 SDK 调用方法”页面提供的
iOS 官方下载包，仅用于支付宝授权登录，不包含支付宝支付接入。

- 官方文档：https://opendocs.alipay.com/open/218/sxc60m?pathHash=49b31754
- 官方下载：https://mdn.alipayobjects.com/portal_khlfqg/afts/file/A*AY2hR6R_dgoAAAAAAAAAAAAAAQAAAQ
- 下载日期：2026-07-26
- `AFServiceSDK` SHA-256：`4FC448891BF3D046F9D9CF3054B819418741C93F3219C3364220394F4C4DCF6C`

升级 SDK 时应重新从官方文档下载，核对公开头文件中的
`AFServiceCenter` / `AFAuthServiceResponse` API，并在真机上验证支付宝回跳。

# 微信 OpenSDK 无支付版

`WechatOpenSDK-NoPay.xcframework` 来自微信开放平台官方 iOS Open SDK 资源页提供的
2.0.5 XCFramework 无支付版。工程仅使用其中的微信登录能力，不包含微信支付实现。

- 官方资源页：https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Downloads/iOS_Resource.html
- 官方下载：https://dldir1.qq.com/WechatWebDev/opensdk/XCFramework/OpenSDK2.0.5_NoPay.zip
- 下载日期：2026-07-27
- 下载包 SHA-256：`28F0EB2AAE2CA35DF6E545811890735FB8798CD31AF99454A7AB2C203DF43864`
- iOS arm64 库 SHA-256：`C9A413FA6BBFEC1C48C6E5906057F096B8FF9BF63B9F5B994FFCDFDF9675326F`
- iOS 模拟器库 SHA-256：`DA70B7F912AB5AC3BD2C91723F415445242BCA113DA02A5588B032B32800A0DB`

无支付版没有对应的微信官方 CocoaPods 依赖，因此由
`iosapp.xcodeproj` 直接链接本目录中的 XCFramework。微信登录还依赖
`applinks:p.ysbit.cn` Associated Domains 权限；发布证书的 App ID 和描述文件也必须
启用 Associated Domains。
