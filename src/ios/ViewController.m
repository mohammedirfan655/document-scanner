//
//  ViewController.m
//  IPDFCameraViewController
//
//  Created by Maximilian Mackh on 11/01/15.
//  Copyright (c) 2015 Maximilian Mackh. All rights reserved.
//

#import "ViewController.h"

#import "IPDFCameraViewController.h"

NSURL *request_url;

@interface ViewController ()

@property (weak, nonatomic) IBOutlet UILabel *titleLabel;
@property (weak, nonatomic) IBOutlet IPDFCameraViewController *cameraViewController;
@property (weak, nonatomic) IBOutlet UIImageView *focusIndicator;
@property (weak, nonatomic) UIApplication *fetchURL;


- (IBAction)focusGesture:(id)sender;

- (IBAction)captureButton:(id)sender;

@end

@implementation ViewController


#pragma mark -
#pragma mark View Lifecycle

- (void)fetchURL:(UIApplication *)fetchURL handleOpenURL:(NSURL *)url {
    // Do something with the url here
    request_url=url;
}


- (void)viewDidLoad
{

    [super viewDidLoad];

    _fetchURL;
    NSLog(@"MI:::Application started");
    
    NSLog(@"MI:::URL recieved %@",request_url);
    
    
    
    [self.cameraViewController setupCameraView];
    [self.cameraViewController setEnableBorderDetection:YES];
    [self updateTitleLabel];
}

- (void)viewDidAppear:(BOOL)animated
{
    [self.cameraViewController start];
}

- (UIStatusBarStyle)preferredStatusBarStyle
{
    return UIStatusBarStyleLightContent;
}

#pragma mark -
#pragma mark CameraVC Actions

- (IBAction)focusGesture:(UITapGestureRecognizer *)sender
{
    if (sender.state == UIGestureRecognizerStateRecognized)
    {
        CGPoint location = [sender locationInView:self.cameraViewController];
        
        [self focusIndicatorAnimateToPoint:location];
        
        [self.cameraViewController focusAtPoint:location completionHandler:^
         {
             [self focusIndicatorAnimateToPoint:location];
         }];
    }
}

- (void)focusIndicatorAnimateToPoint:(CGPoint)targetPoint
{
    [self.focusIndicator setCenter:targetPoint];
    self.focusIndicator.alpha = 0.0;
    self.focusIndicator.hidden = NO;
    
    [UIView animateWithDuration:0.4 animations:^
    {
         self.focusIndicator.alpha = 1.0;
    }
    completion:^(BOOL finished)
    {
         [UIView animateWithDuration:0.4 animations:^
         {
             self.focusIndicator.alpha = 0.0;
         }];
     }];
}

- (IBAction)borderDetectToggle:(id)sender
{
    BOOL enable = !self.cameraViewController.isBorderDetectionEnabled;
    [self changeButton:sender targetTitle:(enable) ? @"CROP On" : @"CROP Off" toStateEnabled:enable];
    self.cameraViewController.enableBorderDetection = enable;
    [self updateTitleLabel];
}

- (IBAction)filterToggle:(id)sender
{
    [self.cameraViewController setCameraViewType:(self.cameraViewController.cameraViewType == IPDFCameraViewTypeBlackAndWhite) ? IPDFCameraViewTypeNormal : IPDFCameraViewTypeBlackAndWhite];
    [self updateTitleLabel];
}

- (IBAction)torchToggle:(id)sender
{
    BOOL enable = !self.cameraViewController.isTorchEnabled;
    [self changeButton:sender targetTitle:(enable) ? @"FLASH On" : @"FLASH Off" toStateEnabled:enable];
    self.cameraViewController.enableTorch = enable;
}

- (void)updateTitleLabel
{
    CATransition *animation = [CATransition animation];
    animation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut];
    animation.type = kCATransitionPush;
    animation.subtype = kCATransitionFromBottom;
    animation.duration = 0.35;
    [self.titleLabel.layer addAnimation:animation forKey:@"kCATransitionFade"];
    
    NSString *filterMode = (self.cameraViewController.cameraViewType == IPDFCameraViewTypeBlackAndWhite) ? @"TEXT FILTER" : @"COLOR FILTER";
    self.titleLabel.text = [filterMode stringByAppendingFormat:@" | %@",(self.cameraViewController.isBorderDetectionEnabled)?@"AUTOCROP On":@"AUTOCROP Off"];
}

- (void)changeButton:(UIButton *)button targetTitle:(NSString *)title toStateEnabled:(BOOL)enabled
{
    [button setTitle:title forState:UIControlStateNormal];
    [button setTitleColor:(enabled) ? [UIColor colorWithRed:1 green:0.81 blue:0 alpha:1] : [UIColor whiteColor] forState:UIControlStateNormal];
}


#pragma mark -
#pragma mark CameraVC Capture Image

- (IBAction)captureButton:(id)sender
{
    __weak __typeof(self) weakSelf = self;
    
    [self.cameraViewController captureImageWithCompletionHander:^(NSString *imageFilePath)
    {
        UIImageView *captureImageView = [[UIImageView alloc] initWithImage:[UIImage imageWithContentsOfFile:imageFilePath]];
        captureImageView.backgroundColor = [UIColor colorWithWhite:0.0 alpha:0.7];
        captureImageView.frame = CGRectOffset(weakSelf.view.bounds, 0, -weakSelf.view.bounds.size.height);
        captureImageView.alpha = 1.0;
        captureImageView.contentMode = UIViewContentModeScaleAspectFit;
        captureImageView.userInteractionEnabled = YES;
        [weakSelf.view addSubview:captureImageView];
        
        // Saving the generated image to PhotoAlbum

//        UIImage *imageFile = [UIImage imageWithContentsOfFile:imageFilePath];
//        UIImageWriteToSavedPhotosAlbum([UIImage imageWithContentsOfFile:imageFilePath], nil, nil, nil);

/*        NSError *error;
        BOOL status = [@"Write me to file" writeToFile:imageFilePath atomically:YES encoding:NSUTF8StringEncoding error:&error];
        NSLog(@"Status=",status);*/
        
        UITapGestureRecognizer *dismissTap = [[UITapGestureRecognizer alloc] initWithTarget:weakSelf action:@selector(dismissPreview:)];
        [captureImageView addGestureRecognizer:dismissTap];
        
        [UIView animateWithDuration:0.7 delay:0.0 usingSpringWithDamping:0.8 initialSpringVelocity:0.7 options:UIViewAnimationOptionAllowUserInteraction animations:^
        {
            captureImageView.frame = weakSelf.view.bounds;
        } completion:nil];
    }];
}

- (void)dismissPreview:(UITapGestureRecognizer *)dismissTap
{
    [UIView animateWithDuration:0.7 delay:0.0 usingSpringWithDamping:0.8 initialSpringVelocity:1.0 options:UIViewAnimationOptionAllowUserInteraction animations:^
    {
        dismissTap.view.frame = CGRectOffset(self.view.bounds, 0, self.view.bounds.size.height);
    }
    completion:^(BOOL finished)
    {
        NSLog(@"MI:::Tapped");
        NSString *customURL = @"d2a://";
        
        if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:customURL]])
        {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:customURL]];
        }
        else
        {
            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Launch Error"
                                                            message:[NSString stringWithFormat:@"D2A Application not found, please install from the App Store"]
                                                           delegate:self cancelButtonTitle:@"Ok"
                                                  otherButtonTitles:nil];
            [alert show];
        }
        
        [dismissTap.view removeFromSuperview];
    }];
}

@end