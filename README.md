[![license](https://img.shields.io/github/license/mathisdt/trackworktime.svg?style=flat)](https://github.com/mathisdt/trackworktime/blob/master/LICENSE)
[![last released](https://img.shields.io/github/release-date/mathisdt/trackworktime.svg?label=last%20released&style=flat)](https://github.com/mathisdt/trackworktime/releases)
[![build](https://github.com/mathisdt/trackworktime/actions/workflows/build.yaml/badge.svg)](https://github.com/mathisdt/trackworktime/actions/)

# Track Work Time
  
This app can track your work time easily! You can automate time tracking using geo-fencing functions (see below).
You may also **categorize each recorded interval** by a predefined client/task and a free text.
Of course, the list of clients/tasks can be edited to suit your needs, and the app has a widget for your home screen.
  
Additionally, if you wish, your **flexible time account is taken care of**: you always see how much you worked.
You can also keep an eye on how much work time is left for today or for the current week (by a **notification**
which you can enable).
  
The app enables you to modify the planned working time effortlessly - just tap on the date you want to edit in the
main table.

You may provide the **geo-coordinates** or the **Wi-Fi network name** of your work place and the app can
**automatically clock you in** while you are at work. This is done **without using GPS**, so your battery won't
be emptied by this app. (You don't have to be connected to the WiFi network at work, it just has to be visible.)

You don't want to open the app for clocking in and out? No problem - there are at least three ways to do that:
add the **widget** to your home screen, use **launcher shortcuts** (long press the app icon for that) or
add a new **quick settings tile** to your panel by tapping on the pencil below and dragging the "Track Work Time"
tile up which then can toggle your clocked-in state.

If you prefer to use other apps like LlamaLab Automate or Tasker for tracking your movements, that's fine - **TWT can
be triggered from other apps** and just do the book-keeping of your work time. In this case, you have to create
broadcast intents with the action *org.zephyrsoft.trackworktime.ClockIn* or *org.zephyrsoft.trackworktime.ClockOut*.
When using ClockIn, you can also set the parameters *task=...* and *text=...* in the "extra" section of
the intent so your events are more meaningful. Here are some screenshots to point out how it can be done in Automate:
[Flow overview](https://zephyrsoft.org/images/automate-1.png),
["Broadcast send" block arguments](https://zephyrsoft.org/images/automate-2.png).
You can also use the action *org.zephyrsoft.trackworktime.StatusRequest* to get the current state of TWT:
is the user clocked in, and if so, with which task and how much time remains for today?
Here's how you can use this in Automate:
[Flow overview](https://zephyrsoft.org/images/automate-3.png),
["Send broadcast" settings top](https://zephyrsoft.org/images/automate-4.png),
["Send broadcast" settings bottom](https://zephyrsoft.org/images/automate-5.png),
["Receive broadcast" settings top](https://zephyrsoft.org/images/automate-6.png),
["Receive broadcast" settings bottom](https://zephyrsoft.org/images/automate-7.png),
["Dialog message" settings](https://zephyrsoft.org/images/automate-8.png),
[resulting message](https://zephyrsoft.org/images/automate-9.png).

It's also possible the other way around: **TWT generates broadcast intents on event creation/update/deletion**.
Automation apps can listen for the actions *org.zephyrsoft.trackworktime.event.Created*,
*org.zephyrsoft.trackworktime.event.Updated* and *org.zephyrsoft.trackworktime.event.Deleted*.  
There are the following extras available: id (number uniquely identifying an event),
date (the event's date, formatted YYYY-MM-DD), time (the event's time, formatted HH\:MM\:SS),
timezone_offset (offset in standard format, e.g. +02:00),
timezone_offset_minutes (offset in minutes, e.g. 120),
type_id (number uniquely identifying the event's type, 0=clock-out / 1=clock-in),
type (name of the event's type, CLOCK_IN or CLOCK_OUT),
task_id (number uniquely identifying the event's task, not available on clock-out events),
task (name of the event's task, not available on clock-out events),
comment (only available if the user provided it),
source (where the event was generated originally, possible values are
MAIN_SCREEN_BUTTON, EVENT_LIST, QUICK_SETTINGS, LAUNCHER_SHORTCUT, MULTI_INSERT, AUTO_PAUSE,
LOCATION, WIFI, RECEIVED_INTENT [includes both externally created broadcasts and actions from TWT's own widget]).  
Some screenshots so you can see it in action in Automate:
[Flow overview](https://zephyrsoft.org/images/automate-receive-1.png),
["Receive Broadcast" settings top](https://zephyrsoft.org/images/automate-receive-2.png),
["Receive Broadcast" settings bottom](https://zephyrsoft.org/images/automate-receive-3.png),
["Dialog Message" settings](https://zephyrsoft.org/images/automate-receive-4.png),
[Result 1](https://zephyrsoft.org/images/automate-receive-5.png),
[Result 2](https://zephyrsoft.org/images/automate-receive-6.png).

If you have a **Pebble** smart watch, the app will notify you on clock-in and clock-out events which is especially
useful if you want to be in the know about automatic time tracking via location and/or WiFi.
  
Finally, the app can generate **reports** for you. The raw events report is the right thing if you want to
import your data somewhere else, while year/month/week reports are fine if you want to keep track of your
task progress.

Important note: **This app definitely won't use your personal data for anything you don't want!**
If the app crashes, it will offer you to send some information about the crash circumstances to the developer
(and does that only if you agree, you will be asked every time). The app does NOT include tracked times or
places in the bug report, but the general log file is appended and might potentially include personal data -
if so, it will be kept strictly confidential and only used to identify the problem.
  
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=org.zephyrsoft.trackworktime)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/org.zephyrsoft.trackworktime/)
  
You can track the past development by looking at the [version history](https://zephyrsoft.org/trackworktime/history).  
  
**This is an open source project**, so if there's something you don't like, you are very welcome to
[file an issue](https://github.com/mathisdt/trackworktime/issues) or even fix things yourself and create a pull request.
Please don't try to communicate with me via reviews, that doesn't work in both directions.
You can always [write me an email](https://zephyrsoft.org/contact-about-me) and I'll see what I can do.
