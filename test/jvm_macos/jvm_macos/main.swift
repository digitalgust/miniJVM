//
//  main.swift
//  jvm_macos
//
//  Created by Gust on 2018/1/30.
//  Copyright © 2018年 Gust. All rights reserved.
//

import Foundation


func execjvm()    {
    
    let mainBundle = Bundle.main.bundlePath
    let app_path=mainBundle.cString(using: String.Encoding.utf8)
    call_jvm(UnsafeMutablePointer<Int8>(mutating: app_path));
}

print("Hello, World!")

execjvm()

