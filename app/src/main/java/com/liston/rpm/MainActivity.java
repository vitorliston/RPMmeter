package com.liston.rpm;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Button;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

public class MainActivity extends Activity implements SensorEventListener  {

    private float lastX, lastY, lastZ;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    FFT2 fft = new FFT2();
    int precision = 512;
    Complex[] ac_valsx = new Complex[precision];
    Complex[] ac_valsy = new Complex[precision];
    Complex[] ac_valsz = new Complex[precision];


    double[] zeros = new double[precision];
    double[] response = new double[precision];
    boolean save = false;

    private int count = 0;
    boolean first = true;
    private long time =System.currentTimeMillis();
    private long last_time =System.currentTimeMillis();;
    BufferedWriter writer;

    private TextView currentX, currentY, currentZ,rate;
    private EditText filename;



    public void savetofile(Context mcoContext, String sBody){
        if (first) {
            first = false;
            String sFileName = filename.getText().toString();
            File dir = new File(Environment.getExternalStorageDirectory(), "RPM_files/"+sFileName+".txt");

            try {
                dir.getParentFile().mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }


            try {


                FileWriter out = new FileWriter(dir);
                writer = new BufferedWriter(out);
                writer.append("t[ms];rpm_x;rpm_y;rpm_z"+"\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
        writer.append(sBody+"\n");


        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        } else {
            // fai! we dont have an accelerometer!
        }

        Button fab = findViewById(R.id.savebutton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save = !save;
                if (save) {
                    fab.setText("Recording");
                    first = true;

                }
                else {
                    fab.setText("Record");

                    try {
                        writer.close();


                    }

                    catch (Exception e) {
                        e.printStackTrace();
                    }

                }


            }
        });


    }

    public void initializeViews() {
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
        rate = (TextView) findViewById(R.id.rate);
        filename = (EditText) findViewById(R.id.filename);


    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public double peak_fr(Complex[] x,double[] y) {

        int index;
        double delta;
        double rate;
        double freq;

        double max;
        double min;
        Complex[] re;

        re=fft.fft(x);

        for ( int i = 1; i < x.length; i++ ) {
            response[i]=re[i].abs();
        }


        delta=0.001*(time-last_time)/precision;
        rate=1/delta;
        freq=rate/precision;
        max=80/freq;

        min=15/freq;
        int d = (int) min;
        index=getIndexOfLargest(response,max,d);

//        Log.d("criarArq", Double.toString(freq));

        return  (int) (index*freq*60);



    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        double[] zero;
        double fx;
        double fy;
        double fz;




        // set the last know values of x,y,z
        lastX = event.values[0];
        lastY = event.values[1];
        lastZ = event.values[2];

        ac_valsx[count]=   new Complex(lastX,0);         // Math.sqrt(lastX*lastX+lastY*lastY+lastZ*lastZ);
        ac_valsy[count]=   new Complex(lastY,0);
        ac_valsz[count]=   new Complex(lastZ,0);

        zeros[count]=0;
        count++;

        if (count==precision) {
            time = System.currentTimeMillis();
            zero=zeros;
            fx=peak_fr(ac_valsx,zero);
            fy=peak_fr(ac_valsy,zero);
            fz=peak_fr(ac_valsz,zero);

            currentX.setText(Double.toString(fx));
            currentY.setText(Double.toString(fy));
            currentZ.setText(Double.toString(fz));

            if (save){
                String sum = Long.toString(time-last_time)+";"+Double.toString(fx)+";"+Double.toString(fy)+";"+Double.toString(fz);

                savetofile(getApplicationContext(),sum);


            }

            count=0;



            rate.setText(Long.toString(time-last_time));
            last_time=time;


        }



    }

    public int getIndexOfLargest( double[] array,double max_ind,int min_ind )
    {
        if ( array == null || array.length == 0 ) return -1; // null or empty

        int largest = 0;
        for ( int i = min_ind; i < array.length; i++ )
        {
            if ( array[i] > array[largest] && i<max_ind ) largest = i;
        }
        return largest; // position of the first largest found
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.rpm100:
                if (checked)
                    precision = 256;
                    break;
            case R.id.rpm50:
                if (checked)
                    precision = 512;
                break;
            case R.id.rpm25:
                if (checked)
                    precision = 1024;
                break;
            case R.id.rpm13:
                if (checked)
                    precision = 2048;
                break;

        }

        ac_valsx = new Complex[precision];
        ac_valsy = new Complex[precision];
        ac_valsz = new Complex[precision];


        zeros = new double[precision];
        response = new double[precision];
        count=0;


    }








}

