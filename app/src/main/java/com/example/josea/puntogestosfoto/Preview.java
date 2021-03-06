package com.example.josea.puntogestosfoto;
/**
 * @author Jose Davis Nidhin
 * https://github.com/josnidhin/Android-Camera-Example
 */
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Clase que permite usar la camara como pantalla para otra aplicación y manejarla correctamente.
 * Código sacado por completo de:
 * https://github.com/josnidhin/Android-Camera-Example
 */
class Preview extends ViewGroup implements SurfaceHolder.Callback {

    // Estructura usada para los mensajes de depuración.
     private final String TAG = "Preview";

    // Estructura para la imagen que se mostrará en la aplicación.
    SurfaceView mSurfaceView;

    // Interfaz para controlar el tamaño de la "superficie"(surfaceview) asignada y el formato; editar píxeles en la "superficie", y monitorizar cambios en la misma.
    SurfaceHolder mHolder;

    // Estructura para guardar el tamaño que está usando la cámara.
    Size mPreviewSize;

    // Lista de tamaños que soporta nuestra cámara.
    List<Size> mSupportedPreviewSizes;

    // Activamos el sensor de la cámara.
    Camera mCamera;

    /**
     * Constructor de la clase, se le asignará lo necesario de la aplicación que use esta clase para poder manejar el surfaceview.
     * @param context parámetro donde se le pasará el contexto de la aplicación que lo use.
     * @param sv parámetro donde se le pasará el objeto surfaceview de la aplicación que lo use.
     */
    Preview(Context context, SurfaceView sv) {
        super(context);

        mSurfaceView = sv;
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Función para asignar la cámara.
     * @param camera parámetro que se le pasará para asignar la cámara que use la aplicación
     */
    public void setCamera(Camera camera) {
    	mCamera = camera;

        //Comprobamos que la cámara está activa.
    	if (mCamera != null) {
            // Si está activa se guarda los tamaños de pantalla que soporta en nuestra lista de tamaños
    		mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();

            // Iniciamos un layout.
    		requestLayout();

    		// Obtenemos los parámetros de la cámara.
    		Camera.Parameters params = mCamera.getParameters();

            // Obtenemos una lista con los modos de enfoque soportados por la cámara
    		List<String> focusModes = params.getSupportedFocusModes();
    		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
    			// Cambiamos el modo de enfoque
    			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    			// Cambiamos los parámetros de la cámara
    			mCamera.setParameters(params);
    		}
    	}
    }

    /**
     * Función para ajustar las dimensiones de la cámara al mostrarla, según la documentación de android encontrada en:
     * http://developer.android.com/intl/es/reference/android/view/View.html#onMeasure%28int,%20int%29
     * Measure the view and its content to determine the measured width and the measured height.
     * This method is invoked by measure(int, int) and should be overridden by subclasses to provide accurate and efficient measurement of their contents.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    /**
     * Función para asignar los parámetros correctos al layout.
     * Según la documentación de android en:
     * http://developer.android.com/intl/es/reference/android/view/View.html#onLayout%28boolean,%20int,%20int,%20int,%20int%29
     * Called from layout when this view should assign a size and position to each of its children.
     * Derived classes with children should override this method and call layout on each of their children.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    /**
     * Función necesaria para iniciar el controlar surfaceholder (necesaria al usar el implements SurfaceHolder.Callback)
     * @param holder parámetro para asignar el controlador
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    /**
     * Función necesaria para destruir el controlador igual que la anterior.
     * @param holder controlador, no se usa pero su cabecera debe ser ésta.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /**
     * Función usada para obtener las dimensiones optimas, llamada por la función onMeasure.
     * @param sizes lista con las dimensiones soportadas por el dispositivo
     * @param w anchura que se obtiene en la función onMeasure usada para calcular la dimensión optima
     * @param h altura que se obtiene en la función onMeasure usada para calcular la dimensión optima
     * @return
     */
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * Función necesaria para controlar los cambios del controlador surfaceholder (necesaria al usar el implements SurfaceHolder.Callback)
     * @param holder controlador que se asignará si ha habido cambios en el mismo, no usado pero necesario en la cabecera
     * @param format parámetro de cambio de formato, no usado pero necesario en la cabecera
     * @param w parámetro que representa la anchura, no usado pero necesario en la cabecera
     * @param h parámetro que representa la altura, no usado pero necesario en la cabecera
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	if(mCamera != null) {
    		Camera.Parameters parameters = mCamera.getParameters();
    		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    		requestLayout();

    		mCamera.setParameters(parameters);
    		mCamera.startPreview();
    	}
    }
}