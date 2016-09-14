# Track Work Time #
  
Track your work time easily with Wifi or via GPS and **categorize each recorded intervall** by a client/task and a free text.  
  
This app can track your work time easily! It lets you categorize each recorded interval by a client/task and a free text. Of course, the list of clients/tasks can be edited to suit your needs.  
  
Additionally, if you wish, your **flexible time account is taken care of**: you always see how much you worked. You can also keep an eye on how much work time is left for today (by a **notification** which you can enable).  
  
You even may provide the **geo-coordinates** or the **Wi-Fi network name** of your work place and the app can **automatically clock you in** while you are at work. This is done **without using GPS**, so your battery won't be emptied by this app.  
  
If you prefer to use other apps like Llama or Tasker for tracking your movements, that's fine - TWT can be triggered from these apps and just do the book-keeping of your work time. In this case, you have to create broadcast intents called **org.zephyrsoft.trackworktime.ClockIn** or **org.zephyrsoft.trackworktime.ClockOut**. When using ClockIn, you can also set the parameters **task=...** and **text=...** in the "extra" section of the intent so your events are more meaningful. Here are some screenshots to point out how it can be done: Llama: [Overview](http://www.zephyrsoft.org/files/llama-1.png), [Detail 1](http://www.zephyrsoft.org/files/llama-2-detail-1.png), [Detail 2](http://www.zephyrsoft.org/files/llama-3-detail-2.png) / Tasker: [Detail 1](http://www.zephyrsoft.org/files/tasker-1.png), [Detail 2](http://www.zephyrsoft.org/files/tasker-2.png).  
  
The latest version even can notify you via your Pebble smart watch on clock-in and clock-out events!  
  
This is an open source project, so if there is something you don't like, you are very welcome to contact me or even write some code yourself and contribute it. Please don't try to communicate with me via reviews on Google Play or AndroidPit, that doesn't work in both directions. Just write me an email, and I'll see what I can do - web site and email address as stated above..  
  
Important note: **This app definitely won't send your personal data anywhere.** It uses the INTERNET permission only to send crash reports, but those reports contain neither any times nor any coordinates you may have tracked.  
  
Track Work Time was migrated from SVN to Git. You can access the old SVN repository here: [Trackworktime in SVN](http://www.zephyrsoft.org/trackworktime)  
You can track the past development by looking at the [version history](http://www.zephyrsoft.org/trackworktime/history).  
  
[Project Tracker (Jira)](https://dev.zephyrsoft.org/jira/browse/TWT)  
  
Links to binary distribution points:  
[TWT @ Google Play Store](https://play.google.com/store/apps/details?id=org.zephyrsoft.trackworktime)  
[TWT @ F-Droid (not maintained actively, possibly older version)](https://f-droid.org/repository/browse/?fdid=org.zephyrsoft.trackworktime)  
