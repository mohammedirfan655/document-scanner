//
//  ViewController.h
//  IPDFCameraViewController
//
//  Created by Maximilian Mackh on 11/01/15.
//  Copyright (c) 2015 Maximilian Mackh. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Cordova/CDV.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVCommandDelegate.h>

@interface ViewController : UIViewController


-(void)test;
//-(void)cordovaResponse:(CDVInvokedUrlCommand*)command;
//-(void)cordovaResponse;

@property (strong, nonatomic) UIWindow *window;

@property (nonatomic, weak) id <CDVCommandDelegate> commandDelegate;

@property (copy) void (^didFinishBlock)(NSString* output);

@end

