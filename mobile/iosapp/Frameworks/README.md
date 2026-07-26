# 支付宝极简版登录 SDK

`AFServiceSDK.framework` 来自支付宝开放平台“极简版 SDK 调用方法”页面提供的
iOS 官方下载包，仅用于支付宝授权登录，不包含支付宝支付接入。

- 官方文档：https://opendocs.alipay.com/open/218/sxc60m?pathHash=49b31754
- 官方下载：https://mdn.alipayobjects.com/portal_khlfqg/afts/file/A*AY2hR6R_dgoAAAAAAAAAAAAAAQAAAQ
- 下载日期：2026-07-26
- `AFServiceSDK` SHA-256：`4FC448891BF3D046F9D9CF3054B819418741C93F3219C3364220394F4C4DCF6C`

升级 SDK 时应重新从官方文档下载，核对公开头文件中的
`AFServiceCenter` / `AFAuthServiceResponse` API，并在真机上验证支付宝回跳。
