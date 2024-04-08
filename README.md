# Zygisk-based reFlutter implementation

reFlutter is a handy tool for reverse engineering Flutter-based applications for both rooted and non-rooted Android. To use Reflutter, you must obtain your APK and replace libflutter.so, resign the APK and reinstall it.

The Zygisk module is designed to empower rooted Android users by simplifying the process. With the provided app, you can easily download `libflutter.so` from reFlutter project. The Zygisk module will then seamlessly replace `libflutter.so` at runtime, making the process straightforward and efficient.

Please note: set up your Proxy IP. Set your Burp Suite like you would set when using reFlutter (listen to `*:8083` and enable "Support invisible proxying").

## Requirements

Rooted Android with Magisk installed and Zygisk Enabled

## Installation

Download the ZIP file, and install it as Zygisk module. You can also do it from ADB:


```
adb push  zygiskreflutter_1.0.zip /sdcard/
adb shell su -c magisk --install-module /sdcard/zygiskreflutter_1.0.zip
adb reboot
```


Install the APK, then setup your proxy IP from "Set Proxy Host" menu.

Select the app that you want to *reFlutter*, click download library, once it is downloaded, you can enable the proxy feature.

You can now start the target app normally


Check adb log and filter it by "ZygiskReflutter" in case you found a problem
