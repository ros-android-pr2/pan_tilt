/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ros.android.pantilt;

import org.ros.exception.RosInitException;
import org.ros.exception.RosNameException;
import org.ros.rosjava.android.OrientationPublisher;
import ros.android.activity.AppManager;
import ros.android.activity.RosAppActivity;
import android.os.Bundle;
import org.ros.Node;
import android.view.Window;
import android.view.WindowManager;
import android.hardware.SensorManager;
import android.util.Log;
import org.ros.ServiceClient;
import org.ros.service.app_manager.StartApp;
import org.ros.ServiceResponseListener;
import android.widget.Toast;
import ros.android.views.SensorImageView;
import org.ros.namespace.NameResolver;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ros.android.util.Dashboard;
import android.widget.LinearLayout;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author pratkanis@willowgarage.com (Tony Pratkanis)
 */
public class PanTilt extends RosAppActivity {
  
  private OrientationPublisher orientationPublisher;
  private String robotAppName;
  private String cameraTopic;
  private SensorImageView cameraView;
  private Dashboard dashboard;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.main);


    robotAppName = getIntent().getStringExtra(AppManager.PACKAGE + ".robot_app_name");
    if( robotAppName == null ) {
      robotAppName = "turtlebot_teleop/android_make_a_map";
    }
    if (getIntent().hasExtra("camera_topic")) {
      cameraTopic = getIntent().getStringExtra("camera_topic");
    } else {
      cameraTopic = "camera/rgb/image_color/compressed_throttle";
    }

    orientationPublisher =
      new OrientationPublisher((SensorManager) getSystemService(SENSOR_SERVICE));
    cameraView = (SensorImageView) findViewById(R.id.image);

    dashboard = new Dashboard(this);
    dashboard.setView((LinearLayout)findViewById(R.id.top_bar),
                      new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 
                                                    LinearLayout.LayoutParams.WRAP_CONTENT));
  }
  
  @Override
  protected void onNodeCreate(Node node) {
    super.onNodeCreate(node);
    try {
      startApp();
      orientationPublisher.main(getNodeConfiguration());
      dashboard.start(node);
    } catch (Exception ex) {
      Log.e("PanTilt", "Init error: " + ex.toString());
      safeToastStatus("Failed: " + ex.getMessage());
    }
  }

  @Override
  protected void onNodeDestroy(Node node) {
    super.onNodeDestroy(node);
    orientationPublisher.shutdown();
    dashboard.stop();
  }


  private void initRos() {
    try {
      Log.i("PanTilt", "getNode()");
      Node node = getNode();
      NameResolver appNamespace = getAppNamespace(node);
      Log.i("PanTilt", "init cameraView");
      cameraView.start(node, appNamespace.resolve(cameraTopic));
      cameraView.post(new Runnable() {
          @Override
          public void run() {
            cameraView.setSelected(true);
          }
        });  
    } catch (RosInitException e) {
      Log.e("PanTilt", "initRos() caught exception: " + e.toString() + ", message = " + e.getMessage());
    }
  }
  
  private void startApp() {
    appManager.startApp(robotAppName,
        new ServiceResponseListener<StartApp.Response>() {
          @Override
          public void onSuccess(StartApp.Response message) {
            initRos();
            // TODO(kwc): add status code for app already running
            /*
             * if (message.started) { safeToastStatus("started"); initRos(); }
             * else { safeToastStatus(message.message); }
             */
          }

          @Override
          public void onFailure(Exception e) {
            safeToastStatus("Failed: " + e.getMessage());
          }
        });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.pan_tilt_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.kill:
      android.os.Process.killProcess(android.os.Process.myPid());
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  private void safeToastStatus(final String message) {
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(PanTilt.this, message, Toast.LENGTH_SHORT).show();
        }
      });
  }
  
}