#import <Cordova/CDV.h>
#import "ViewController.h"

@interface DocumentScanner : CDVPlugin

- (void) pluginInitialize;

- (void) process:(CDVInvokedUrlCommand*)command;

@end