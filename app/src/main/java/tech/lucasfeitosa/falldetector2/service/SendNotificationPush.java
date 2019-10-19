package tech.lucasfeitosa.falldetector2.service;

import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import tech.lucasfeitosa.falldetector2.App;
import tech.lucasfeitosa.falldetector2.activity.RegisterActivity;

import static android.content.ContentValues.TAG;

public class SendNotificationPush {

    public static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private String serverKey =
            "key=" + "AAAA21VUkcc:APA91bFEbKe73ki_rz-utRa_RK6A14OJGQetmJ_gvO1WoFFoa-lN1b-ELHsQlehBWZY6YnqwTbqFRXSAV6kwRe4Yz2mE9UFvdmwgHL7b6HHY1Wkup0YGoGuvB-LMaK3NzQUid9i0K8jG";
    private String contentType = "application/json";

    public void sendNotification(String message){
        String topic = "falldetector";
        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("title", "Queda detectada");
            notificationBody.put("message", message);  //Enter your notification message
            notification.put("to", topic);
            notification.put("data", notificationBody);
            Log.d(TAG, "sendNotification: " + notification.toString());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        sendPush(notification);
    }

    public void sendPush(JSONObject notification) {

        RequestQueue requestQueue = Volley.newRequestQueue(App.getInstance().getApplicationContext());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCM_API, notification,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, "onErrorResponse: Didn't work");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", serverKey);
                params.put("Content-Type", contentType);
                return params;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }
}

