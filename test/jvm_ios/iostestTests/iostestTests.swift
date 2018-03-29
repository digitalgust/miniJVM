//
//  iostestTests.swift
//  iostestTests
//
//  Created by Gust on 2017/10/6.
//  Copyright © 2017年 Gust. All rights reserved.
//

import XCTest
@testable import iostest

class iostestTests: XCTestCase {
    
    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
        let myString = "in Tests!"
        print(myString)
        

        let mainBundle = Bundle.main.bundlePath
        let app_path=mainBundle.cString(using: String.Encoding.utf8)
        call_jvm(UnsafeMutablePointer<Int8>(mutating: app_path));

    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }
    
    func testExample() {
        // This is an example of a functional test case.
        // Use XCTAssert and related functions to verify your tests produce the correct results.
    }
    
    func testPerformanceExample() {
        // This is an example of a performance test case.
        self.measure {
            // Put the code you want to measure the time of here.
        }
    }
    
}
