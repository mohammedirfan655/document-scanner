#import <Cordova/CDV.h>
#import "ViewController.h"

@interface DocumentScanner : CDVPlugin

- (void) pluginInitialize;

- (void) process:(CDVInvokedUrlCommand*)command;

@property (strong, nonatomic) UIWindow *window;

@property UIViewController *presentViewController;
@property (nonatomic, retain) ViewController *viewc;

@end