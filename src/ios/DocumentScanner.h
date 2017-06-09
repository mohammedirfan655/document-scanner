#import <Cordova/CDV.h>

@interface DocumentScanner : CDVPlugin

- (void) pluginInitialize;

- (void) process:(CDVInvokedUrlCommand*)command;

@end