When Moving
====

I wanted to track my GPS coordinates in a battery-efficient manner. This app only polls GPS when it detects that you are moving. It stores coordinates locally in an SQLite database.

Note: I still haven't added back Google Maps integration. Sorry.

How does it work?
====

With a default tracking interval of 40 seconds, a GPS timeout of 20 seconds, and an accelerometer threshold of 0.3 ...

The app checks the accelerometer every 40 seconds, for 2 seconds. If 30% of the readings during those 2 seconds register an absolute force vector greater than the current accelerometer threshold, we're moving. At which point we poll for GPS. GPS polling stops once we've reached our timeout of 20 seconds, or we get a reading accurate to within 1 meter.

Battery drain ultimately depends on how often you're actually moving. The more often it polls for GPS, the bigger drain. But the accelerometer polling goes relatively unnoticed by the system.

Thanks
====

http://www.iconfinder.com/icondetails/15696/128/buried_map_trail_treasure_treasure_map_x_icon

Journal
====

2012-07-06

Finally worked through a gotcha with WakeLocks. A partial wake lock will stay alive during GPS polling, but will not keep the CPU awake during accelerometer polling. Fought with this for more than a week! The workaround is to create a screen-dim + acquire-causes-wakeup lock. All better!

2012-07-11

Next plans are to:

* Add a timeout for GPS polling
* Only store the most accurate locations. Think I can tell the OS to only return fixes with at least a certain level of accuracy.
* Perhaps: Add a map to the activity and show current location

2012-07-14

GPS timeout is mostly working. Still quirky ... too tired for any more tonight.

2012-07-24

Forgot to store the best location when GPS times out.

2012-07-26

Preparing to use standard deviation of GPS accuracies as a measure of whether we should stop polling. Thinking that if we're under 20 meters of accuracy, and std-dev is around 3, we probably won't get much better and should stop.

2012-07-27

Actually, I think standard deviation is overkill. Should be able to accomplish the same by keeping the last 5 location fixes, and comparing the maximum and minimum accuracy values. If their difference is less than 5 meters, it's unlikely accuracy will greatly improve much more.

And after riding the Portland streetcar, perhaps I should lower the threshold for "significant acceleration". Opened Accelerometer Monitor and it didn't get much above 0.2 (accel vector minus gravity). Maybe 0.3 will be sufficient.

2012-07-30

Next I want a toggle button for turning the service on/off. Usefuly for nighttime suspensions, or other long durations when I won't be moving. Not certain whether this should be a toggle above the map, or elsewhere.

2012-08-04

few more ideas:

* If we were moving last time, poll GPS once more to get our stopping location. Otherwise it could be a minute off. Plus, it might just be a fluke that we appear to be stopped.

2012-08-09

When activity starts, need a way to check whether the service is running so we can show the proper checkbox state. There have been instances where the service is running but checkbox doesn't know about it.

Might like to use speed reported by GPS to determine next poll time. If we're moving fast, might poll as frequently as every 10 seconds, where normally it'd be 30 to 60.

Should do a write-up of what I've learned about GPS and how to get what you want out of it.

2012-08-21

Still have to figure out why the service crashes with 3G goes away. Perhaps something with polling for coarse location. Or maybe the callback about location service changes isn't being handled correctly.

2012-08-23

If no GPS, make sure cell tower location will work, so need to tweak circular buffer logic.

2012-09-02

It no longer crashes when 3G goes away, but is quirky in other ways. Such is the life of a long-running service, I guess. Maybe there's a better way.
