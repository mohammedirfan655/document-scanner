#import "DocumentScanner.h"
#import "ViewController.h"
#import <AVFoundation/AVFoundation.h>


@interface DocumentScanner()

@end

@implementation DocumentScanner

NSString *_routeChangedCallbackId;

- (void) pluginInitialize {
    NSLog(@"DocumentScanner:pluginInitialized");
    _routeChangedCallbackId = nil;
}

- (void) process:(CDVInvokedUrlCommand*)command
{
    NSLog(@"DocumentScanner:process");
    
    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
    ViewController * docViewController = [storyboard   instantiateViewControllerWithIdentifier:@"iController"] ;

    DocumentScanner * __weak weakSelf = self;
    
    docViewController.didFinishBlock = ^(NSString *output){
        NSLog(@"DocumentScanner:didFinishBlock");
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:output];
        [weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    };
    
    [self.viewController presentViewController:docViewController animated:YES completion:nil];

    
//   CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"test"];
//   [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end