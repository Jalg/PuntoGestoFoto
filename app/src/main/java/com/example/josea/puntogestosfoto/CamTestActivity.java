package com.example.josea.puntogestosfoto;

/*
Autores:
    José Miguel Navarro Moreno
    José Antonio Larrubia García
*/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;

/// <summary>
/// Clase principal de la aplicación, cámara de fotos que hará una foto a los 3 segundos de reconocer el patrón dibujado en pantalla adecuado.
/// Se ha usado la siguiente documentación:
/// Parte 2.5  y 3 de : http://nuevos-paradigmas-de-interaccion.wikispaces.com/Detecci%C3%B3n+de+patrones+en+Android+-+Gesture+Builder
/// para la realización de la parte del patrón.
/// https://github.com/josnidhin/Android-Camera-Example
/// como base para la cámara.
/// </summary>
public class CamTestActivity extends Activity implements GestureOverlayView.OnGesturePerformedListener {

    /// <summary>
    /// Variable usada para los mensajes de depuración.
    /// </summary>
    private static final String TAG = "CamTestActivity";

    /// <summary>
    /// Estructura que usa la cámara para poder usarla sin problema, ver su archivo para más información.
    /// </summary>
    Preview preview;

    /// <summary>
    /// Sensor de la cámara.
    /// </summary>
    Camera camera;

    /// <summary>
    /// Estructura para la actividad, asignada por la superclase.
    /// </summary>
    Activity act;

    /// <summary>
    /// Estructura para el contexto de la aplicación, asignada por la superclase.
    /// </summary>
    Context ctx;

    /// <summary>
    /// Estructura para los gestos.
    /// </summary>
    private GestureLibrary gestureLib;

    /// <summary>
    /// Función que se llama al iniciar la aplicación, inicia todos los servicios necesarios.
    /// </summary>
    /// <param name=" savedInstanceState "> Parámetro con la instancia de la aplicación, pasado por android </param>
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        act = this;

        //Ponemos que no nos aparezca el titulo de la aplicación en el layout.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        // Iniciamos lo que se requiere para los gestos, importante iniciarlos antes que los de la cámara.
        PrepararGestos();

        // Iniciamos lo necesario para la cámara usando la clase Preview.
        preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
        preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.layout)).addView(preview);
        preview.setKeepScreenOn(true);
    }

    /// <summary>
    /// Función llamada por android al reanudar la aplicación después de estar en pausa para recuperarse.
    /// </summary>
    @Override
    protected void onResume() {
        super.onResume();
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open(0);
                camera.startPreview();
                preview.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    /// <summary>
    /// Función llamada cuando se pause la aplicación.
    /// </summary>
    @Override
    protected void onPause() {
        if(camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
        super.onPause();
    }

    /// <summary>
    /// Función que reinicia la cámara después de realizar una foto.
    /// </summary>
    /// <param name=" ">  </param>
    private void resetCam() {
        camera.startPreview();
        preview.setCamera(camera);
    }

    /// <summary>
    /// Función para actualizar la galeria de fotos del dispositivo.
    /// </summary>
    /// <param name=" file "> fichero con el que se actualizará la galería </param>
    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    /// <summary>
    /// Estructura necesaria para la cámara cuando se realiza la foto.
    /// </summary>
    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
        }
    };

    /// <summary>
    /// Estructura necesaria para la cámara cuando se realiza la foto.
    /// </summary>
    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };

    /// <summary>
    /// Estructura necesaria para la cámara cuando se realiza la foto, en esta si hacemos algo más que en las anteriores,
    /// guardamos la imagen correctamente y reiniciamos la cámara.
    /// </summary>
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    /// <summary>
    /// Clase para guardar correctamente la imagen.
    /// </summary>
    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        /// <summary>
        /// Función para que al hacer la foto la almacene correctamente.
        /// </summary>
        /// <param name=" data "> parámetro que contiene los datos de la imagen </param>
        @Override
        protected Void doInBackground(byte[]... data) {
            // Inicializamos el flujo de salida.
            FileOutputStream outStream = null;

            // guardamos la imagen en la sd.
            try {
                File sdCard = Environment.getExternalStorageDirectory();

                //Si no existe el directorio donde se almacenan las fotos de la aplicación se crea
                File dir = new File (sdCard.getAbsolutePath() + "/app_gesto_foto");
                dir.mkdirs();

                //Iniciamos el nombre que tendra el fichero y lo creamos.
                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                //Guardamos el flujo en en el fichero de salida.
                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                //Actualizamos la galeria con el nuevo fichero.
                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

    }

    /// <summary>
    /// Función llamada por onCreate para inicializar correctamente lo necesario para el analizador de gestos.
    /// </summary>
    public void PrepararGestos(){
        GestureOverlayView gestureOverlayView = new GestureOverlayView(this);
        View inflate = getLayoutInflater().inflate(R.layout.main, null);
        gestureOverlayView.addView(inflate);
        gestureOverlayView.addOnGesturePerformedListener(this);
        gestureLib = GestureLibraries.fromRawResource(this, R.raw.gestures);

        if (!gestureLib.load()) {
            finish();
        }
        setContentView(gestureOverlayView);
    }

    /// <summary>
    /// Función que detecta si el patrón que se realiza en pantalla coincide con alguno de los almacenados.
    /// </summary>
    /// <param name=" overlay "> parámetro para leer desde pantalla el gesto, necesario para el reconocimiento aunque no lo usemos concretamente </param>
    /// <param name=" gesture "> parámetro que almacena el gesto realizado en pantalla por el usuario  </param>
    public void onGesturePerformed (GestureOverlayView overlay, Gesture gesture){
        //Manejador para hacer que tome la foto en 3 segundos
        Handler mhandler = new Handler();

        //Lista que guarda lo parecido que es el patrón hecho en pantalla por el usuario con el almacenado por la aplicación.
        ArrayList<Prediction> predictions = gestureLib.recognize(gesture);

        for (Prediction prediction : predictions) {
            // Comprobamos si alguno de los gestos introducidos por pantalla es parecido al nuestro.
            // Hemos elegido un score de 4 porque ni era lo suficiente sensible para que no detectará ningún gesto como parecido ni
            // lo suficiente tosco para que detectara cualquier patrón como si fuera el nuestro, calculado haciendo pruebas.
            if (prediction.score > 4.0) {
                //Mostramos un mensaje en pantalla
                Toast.makeText(this,getString(R.string.Foto_en_3) , Toast.LENGTH_SHORT).show();

                //Hacemos la foto cuando pasan 3 segundos.
                mhandler.postDelayed(new Runnable() {
                    public void run() {
                        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                        Toast.makeText(ctx, getString(R.string.Foto_realizada), Toast.LENGTH_SHORT).show();
                    }
                }, 3000);
            }
        }
    }
}