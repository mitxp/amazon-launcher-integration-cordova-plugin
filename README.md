## Cordova plugin for Fire TV Launcher Integration

The Cordova plugin for Fire TV Launcher Integration makes it possible to integrate your HTML5 webapp with the Fire TV launcher. After integrating with the Fire TV Catalog, this plugin enables users to launch content in your app directly from search.

## Instructions 

You can find the instructions for the plugin located here: https://developer.amazon.com/docs/fire-tv/cordova-plugin-catalog.html

## License

This library is licensed under the Apache 2.0 License. 

## Changes made in this fork

- Added onNewIntent lifecycle method to handle deeplinks while this app is already running
- Added a plugin installation variable INTENT_DATA_HOST (set with --variable) which will add a data element to the intent-filter in the Android manifest with a host attribute using that value and a scheme set to https.
- Removed allowed character list check ("a-zA-Z0-9\\-_:") when handling an intent in the plugin's Activity class.
- Apply some of the plugin patches in a new before_compile hook. This seems necessary because other plugins revert changes during the build phase that were made by this plugin during installation.