# ROS2 Joystick — Android app to drive turtlesim (ROS2 Jazzy)

An Android joystick that connects to ROS2 over **rosbridge** and:

1. Publishes `geometry_msgs/Twist` to `/turtle1/cmd_vel` to move the turtle.
2. Publishes `std_msgs/String` ("UP"/"DOWN"/"LEFT"/"RIGHT"/"CENTER") to
   `/joystick/direction` so you can `ros2 topic echo` which arrow is pressed.

No native ROS2 client library is needed on Android — it talks to
`rosbridge_server` over a plain WebSocket, which is the standard way to
bridge ROS2 to phones/web apps.

## Architecture

```
[Android Joystick App] --ws://<PC-IP>:9090--> [rosbridge_server] --> ROS2 Jazzy --> [turtlesim]
```

## Part 1 — ROS2 side (Ubuntu with ROS2 Jazzy)

1. Install turtlesim and rosbridge:
   ```bash
   sudo apt update
   sudo apt install ros-jazzy-turtlesim ros-jazzy-rosbridge-suite
   source /opt/ros/jazzy/setup.bash
   ```

2. Terminal A — start turtlesim:
   ```bash
   ros2 run turtlesim turtlesim_node
   ```

3. Terminal B — start rosbridge (default port 9090):
   ```bash
   ros2 launch rosbridge_server rosbridge_websocket_launch.xml
   ```

4. Terminal C — find your PC's LAN IP (the phone needs this):
   ```bash
   hostname -I
   ```
   Make sure your phone and PC are on the same Wi-Fi network, and that
   your firewall allows inbound TCP on port 9090:
   ```bash
   sudo ufw allow 9090/tcp   # if ufw is enabled
   ```

5. (Optional, for the video) Watch the topics live while you test:
   ```bash
   ros2 topic echo /turtle1/cmd_vel
   ros2 topic echo /joystick/direction
   ```

## Part 2 — Android app

1. Open this folder (`RosJoystick/`) in Android Studio as an existing project
   and let Gradle sync.

2. Files of interest:
   - `JoystickView.kt` — custom `View` that draws the joystick and reports
     normalized `x`/`y` in `[-1, 1]` (up = +y, right = +x).
   - `RosBridgeClient.kt` — tiny OkHttp WebSocket wrapper that speaks the
     rosbridge v2 JSON protocol (`advertise` / `publish` ops).
   - `MainActivity.kt` — wires the joystick to the client: converts
     `x,y` into a `Twist`, throttles publishing to ~20 Hz, and detects which
     of the 4 directions (or center) is active for the direction topic.

3. Run the app on a real phone (recommended for a smooth demo — the
   emulator's virtual joystick input can feel laggy on camera) or an
   emulator with network access to your PC.

4. In the app, enter the rosbridge URL, e.g.:
   ```
   ws://192.168.1.42:9090
   ```
   and tap **Connect**. Status should change to "Connected: ...".

5. Drag the on-screen joystick — the turtle in the `turtlesim` window should
   move, and the "Direction" label plus `/joystick/direction` topic should
   update as you cross into each zone (dead-zone of 25% in the center).

## Tuning

- `MainActivity.MAX_LINEAR_SPEED` / `MAX_ANGULAR_SPEED` — top speed sent to
  the turtle.
- `deadZone` in `updateDirection()` — how far from center before a
  direction registers.
- Publish rate — change the `50` (ms) in `handler.postDelayed(this, 50)`
  for a faster/slower `cmd_vel` stream.
