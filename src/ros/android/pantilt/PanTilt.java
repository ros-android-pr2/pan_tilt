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

import org.ros.android.OrientationPublisher;
import ros.android.activity.RosAppActivity;
import android.os.Bundle;
import org.ros.node.Node;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;
import ros.android.views.SensorImageView;
import org.ros.namespace.NameResolver;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.ros.node.NodeRunner;
import org.ros.node.NodeConfiguration;
import org.ros.node.DefaultNodeRunner;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author pratkanis@willowgarage.com (Tony Pratkanis)
 */
public class PanTilt extends RosAppActivity {
  
  private OrientationPublisher orientationPublisher;
  private String robotAppName;
  private String cameraTopic;
  private SensorImageView cameraView;
  private NodeRunner nodeRunner;

  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setDefaultAppName("pr2_pan_tilt/pr2_pan_tilt");
    setDashboardResource(R.id.top_bar);
    setMainWindowResource(R.layout.main);
    super.onCreate(savedInstanceState);

    nodeRunner = DefaultNodeRunner.newDefault();


    if (getIntent().hasExtra("camera_topic")) {
      cameraTopic = getIntent().getStringExtra("camera_topic");
    } else {
      cameraTopic = "camera/rgb/image_color/compressed_throttle";
    }

    orientationPublisher =
      new OrientationPublisher((SensorManager) getSystemService(SENSOR_SERVICE));
    cameraView = (SensorImageView) findViewById(R.id.image);
  }
  
  @Override
  protected void onNodeCreate(Node node) {
    super.onNodeCreate(node);
    try { 
      NodeConfiguration nc = NodeConfiguration.copyOf(getNodeConfiguration());
      nc.setNodeName(node.getName() + "_pan_tilt");
      nodeRunner.run(orientationPublisher, nc);
      NameResolver appNamespace = getAppNamespace(node);
      cameraView.start(node, appNamespace.resolve(cameraTopic).toString());
      cameraView.post(new Runnable() {
          @Override
          public void run() {
            cameraView.setSelected(true);
          }
        });  
    } catch (Exception e) {
      final Exception ex = e;
      Log.e("PanTilt", "Init error: " + ex.toString());
      ex.printStackTrace();
      runOnUiThread(new Runnable() {
          @Override
            public void run() {
            AlertDialog d = new AlertDialog.Builder(PanTilt.this).setTitle("Error!").setCancelable(false)
              .setMessage("Failed: cannot contact robot:" + ex.toString())
              .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) { }})
              .create();
            d.show();
          }});
    }
  }

  @Override
  protected void onNodeDestroy(Node node) {
    super.onNodeDestroy(node);
    nodeRunner.shutdownNodeMain(orientationPublisher);
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
}