Ampdroid
--------
Ampdroid by Daniel Schrühl (@meandor) is an Android Client for the music streaming server Ampache (http://ampache.org/). The original repository is listed on GitHUB here: https://github.com/meandor/ampdroid

This special version is optimized for the use with ownCloud. It has been converted to Android Studio and a list of problems with ownCloud have been fixed.

This app is designed to play music even when Encryption 2.0 app is activated. The specific user-credentials for decrypting the files have to be set within the app.

Usage
--------
1. Install and configure the ownCloud-music-app
2. Download and install the APK-file out of this repository (https://github.com/xn--nding-jua/Ampdroid/blob/master/app/app-release.apk?raw=true). It has been compiled for Android >=4.0.3
3. Enter the path to the music app: "https://cloud.domain.de/index.php/apps/music/ampache" (server/xml.server.php will be added automatically)
4. Enter credentials for ownCloud (User/Password) and the generated password for the ampache-server
5. enjoy the music

Hints
--------
As with ownCloud version 8.1 streaming on encrypted content seems not to be possible, the app will download each file before playing to the temporary file "/sd-card/tmp/Ampdroid.mp3" first. Afterwards it will be played back.

Warning
--------
As the user-credentials are stored unencrypted within the config-file, these information should be entered only on private-phones!

Changelog
--------
- Changed app so that Android API v15 is supported now as I want to use my old Android 4.0.3 phone ;-)
- Fixed the calculation of HEX-auth-passphrase to meet ownCloud-music-app-passphrase
- Fixed parsing of XML-content. perseInt() failed as ownCloud does not send "0" but "" e.g. for TrackNr or Year
- Changed authentication to ownCloud credentials for decrypting music-content
- Added Download-functionality as encrypted ownCloud-content cannot be streamed. So first the audio-file will be downloaded using basic authentication (please use https!!!) and played afterwards. A new notifier will give feedback.
- Fixed german translation
- Several smaller fixes here and there :)
