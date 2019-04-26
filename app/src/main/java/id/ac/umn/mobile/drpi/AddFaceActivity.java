package id.ac.umn.mobile.drpi;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.renderscript.Sampler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class AddFaceActivity extends Dialog implements View.OnClickListener{

    SharedPreferences prefs = getContext().getSharedPreferences("DOORPI", Context.MODE_PRIVATE);
    String ipDest = prefs.getString("ip destination", "");

    private Activity c;
    private String default_url = "http://" + ipDest + ":5000";
//    private String default_url = "http://192.168.137.54:5000/";
    private String ApiUrl = default_url + "/api/addface";
    private String existed = "false";
    private String nameData = "", userData;



    AddFaceActivity(Activity a) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_add_face);
        final ProgressBar progressBar = findViewById(R.id.marker_progress);
        final EditText addNewFaceET = findViewById(R.id.addNewFaceET);
        final Button addUpdateNameBtn = findViewById(R.id.addUpdateNameBtn);
        final Spinner knownNamesSpinner = findViewById(R.id.knownNamesSpn);
        final Spinner userDataSpinner = findViewById(R.id.userDataSpn);
        final LinearLayout linearLayout = findViewById(R.id.addFaceLL);
        addUpdateNameBtn.setOnClickListener(this);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(default_url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiServiceNames service = retrofit.create(ApiServiceNames.class);
        Call<JsonObject> jsonCall = service.readJson();

        String identity = prefs.getString("identity", "");
        if (identity.equals("unknown")){
            knownNamesSpinner.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
        }
        else{
            addNewFaceET.setVisibility(View.GONE);
            jsonCall.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Gson gson = new Gson();
                    Map<String, Object> myMap = gson.fromJson(response.body(), type);

                    //Put response into Spinner
                    String str = myMap.get("names").toString().replace("[", "").replace("]", "");
                    String[] strArray = str.split(" ");
                    StringBuilder builder = new StringBuilder();
                    for (String s : strArray){
                        String cap = s.substring(0,1).toUpperCase() + s.substring(1);
                        builder.append(cap).append(" ");
                    }
                    str = builder.toString();
                    Log.i("Response", str);

                    String[] elements = str.split(", ");
                    List<String> myList = Arrays.asList(elements);
                    ArrayList<String> listOfString = new ArrayList<String>(myList);

                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listOfString);
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    knownNamesSpinner.setAdapter(dataAdapter);

                    progressBar.setVisibility(View.GONE);
                    linearLayout.setVisibility(View.VISIBLE);
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("Failure", t.toString());
                }
            });
        }

        addNewFaceET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() !=0 ){
                    knownNamesSpinner.setEnabled(false);
                    addUpdateNameBtn.setText("Add Name");
                    addUpdateNameBtn.setEnabled(true);
                    existed = "false";
                }
                else{
                    knownNamesSpinner.setEnabled(true);
                    addUpdateNameBtn.setText("Add/Update Name");
                    addUpdateNameBtn.setEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        knownNamesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (knownNamesSpinner.getSelectedItemPosition() != 0){
                    addNewFaceET.setEnabled(false);
                    addUpdateNameBtn.setText("Update Name");
                    addNewFaceET.setHintTextColor(Color.GRAY);
                    addUpdateNameBtn.setEnabled(true);
                    existed = "true";
                }
                else{
                    addNewFaceET.setEnabled(true);
                    addUpdateNameBtn.setText("Add/Update Name");
                    addNewFaceET.setHintTextColor(Color.LTGRAY);
                    addUpdateNameBtn.setEnabled(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        addUpdateNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (existed.equals("false")){
                    // nama sudah ada di face database
                    nameData = addNewFaceET.getText().toString();
                }
                else if (existed.equals("true")){
                    // nama baru
                    nameData = knownNamesSpinner.getSelectedItem().toString();
                }
                userData = userDataSpinner.getSelectedItem().toString();
                userData = userData.substring(0,1).toLowerCase() + userData.substring(1).replace(" ","");
                Log.d("user data", userData);
                send();
                addUpdateNameBtn.setEnabled(false);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }









    // Edit ini aja
    private String HttpPost(String myUrl) throws IOException, JSONException {
        URL url = new URL(myUrl);

        // 1. create HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        // 2. build JSON object
        JSONObject jsonObject = buidJsonObject();

        // 3. add JSON content to POST request body
        setPostRequestContent(conn, jsonObject);

        // 4. make POST request to the given URL
        conn.connect();

        // 5. Get JSON response from server
        StringBuilder JSONresult = new StringBuilder();
        InputStream in = new BufferedInputStream(conn.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while((line = reader.readLine()) != null){
            JSONresult.append(line);
        }

        // 6. Convert JSON to Map
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Gson gson = new Gson();
        Map<String, Object> myMap = gson.fromJson(JSONresult.toString(), type);

        // 6. return response message
        Date currentTime = Calendar.getInstance().getTime();

        String identitas;
        try{
            identitas = myMap.get("identity").toString();
        }
        catch (Exception e){
            identitas = "error not found!";
        }

        return myMap.get("state").toString();
    }

    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                try {
                    return HttpPost(urls[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return "Error!";
                }
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d("", "Tes POST dari sini");
            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    private void send() {
        new HTTPAsyncTask().execute(ApiUrl);
    }

    // Edit ini aja
    private JSONObject buidJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("exist", existed);
        jsonObject.accumulate("name",  nameData);
        jsonObject.accumulate("userdata", userData);

        return jsonObject;
    }

    private void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(jsonObject.toString());
        Log.i(MainActivity.class.toString(), jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addUpdateNameBtn:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getContext(), "WOLOLO", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        dismiss();
    }
}
