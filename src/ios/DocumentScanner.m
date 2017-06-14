#import "DocumentScanner.h"
#import "ViewController.h"
#import <AVFoundation/AVFoundation.h>
#import <Cordova/CDVViewController.h>


@interface DocumentScanner()

    @property (nonatomic, retain) ViewController *vc;
@end

@implementation DocumentScanner

NSString *_routeChangedCallbackId;
@synthesize viewc;


- (void) pluginInitialize {
    NSLog(@"DocumentScanner:pluginInitialize");

    _routeChangedCallbackId = nil;
}

- (void) process:(CDVInvokedUrlCommand*)command
{
    
    
//    ViewController *listingVC = [[ViewController alloc] init];
//    [(UINavigationController *)self.window.rootViewController pushViewController:listingVC animated:YES];
//    [(UINavigationController *)[UIApplication sharedApplication].keyWindow.rootViewController popViewControllerAnimated:YES];

   
    
//    ViewController *myViewController = [storyboard instantiateViewControllerWithIdentifier:@"iController"];
//    [ViewController loadCustomData:myCustomData];
//    [ViewController viewDidLoad:myViewController animated:YES completion:nil];

    
    
//   CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"test"];

//   [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end