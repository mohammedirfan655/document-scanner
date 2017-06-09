#import "DocumentScanner.h"
#import "ViewController.h"
#import <AVFoundation/AVFoundation.h>


@interface DocumentScanner()

    @property (weak, nonatomic) IBOutlet ViewController *viewc;
@end

@implementation DocumentScanner
{
    NSString *_routeChangedCallbackId;
}

- (void) pluginInitialize {
    NSLog(@"DocumentScanner:pluginInitialize");

    _routeChangedCallbackId = nil;
}

- (void) process:(CDVInvokedUrlCommand*)command
{

   CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"test"];

   [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end