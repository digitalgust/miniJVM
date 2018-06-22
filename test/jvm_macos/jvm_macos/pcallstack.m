//
//  pcallstack.m
//  jvm_macos
//
//  Created by Gust on 2018/6/22.
//  Copyright © 2018年 Gust. All rights reserved.
//

#import <Foundation/Foundation.h>


void print_stack(){
    NSLog(@"%@", [NSThread callStackSymbols]);
}
