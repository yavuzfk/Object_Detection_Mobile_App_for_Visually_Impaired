package org.yavuz.ai.computervision.objectdetection;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.tensorflow.lite.examples.detection.R;
import org.yavuz.ai.computervision.objectdetection.env.ImageUtils;
import org.yavuz.ai.computervision.objectdetection.env.Logger;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;


public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
//  private BottomSheetBehavior<LinearLayout> sheetBehavior;

  protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
  private ImageView plusImageView, minusImageView;
  private SwitchCompat apiSwitchCompat;
  private TextView threadsTextView;

  public static String[] taninan_neseler = {"insan","bisiklet","araba","motosiklet","uçak"
          ,"otobüs"
          ,"tren"
          ,"kamyon"
          ,"gemi"
          ,"trafik lambası"
          ,"yangın musluğu"
          ,"dur işareti"
          ,"parkmetre"
          ,"oturma bankı"
          ,"kuş"
          ,"kedi"
          ,"köpek"
          ,"at"
          ,"kuzu"
          ,"inek"
          ,"fil"
          ,"ayı"
          ,"zebra"
          ,"zurafa"
          ,"çanta"
          ,"şemsiye"
          ,"el çantası"
          ,"kravat"
          ,"bavul"
          ,"frizbi"
          ,"kayaklar"
          ,"kayak"
          ,"top"
          ,"kaykay"
          ,"bezbol şapkası"
          ,"bezbol topu"
          ,"kaykay"
          ,"sörf tahtası"
          ,"tenis raketi"
          ,"şişe"
          ,"şarap bardağı"
          ,"bardak"
          ,"çatal"
          ,"bıçak"
          ,"kaşık"
          ,"tabak"
          ,"muz"
          ,"elma"
          ,"sandwich"
          ,"portakal"
          ,"brokoli"
          ,"havuç"
          ,"hot dog"
          ,"pizza"
          ,"donut"
          ,"kek"
          ,"sandalye"
          ,"kanape"
          ,"bitki"
          ,"yatak"
          ,"yemek masası"
          ,"tuvalet"
          ,"televizyon"
          ,"laptop"
          ,"fare"
          ,"kumanda"
          ,"klavye"
          ,"telefon"
          ,"mikrodalga"
          ,"fırın"
          ,"tost makinesi"
          ,"lavabo"
          ,"buzdolabı"
          ,"kitap"
          ,"saat"
          ,"vazo"
          ,"makas"
          ,"oyuncak ayı"
          ,"fön makinesi"
          ,"diş fırçası"};


  public static TextToSpeech mTTs;
  SpeechRecognizer recognizer;

  public Intent intent;

  public static String sagda;
  public static String solda;
  public static String ortada;
  public static String gorunuyor;
  public static String bulunamadi;
  public static String uzakta;
  public static String yakinda;

  public static String aranan = "error var";

  public static boolean kesfetBasla = false;
  public static boolean aramaBasla = false;
  public static boolean BelirliArama = false;
  public static boolean SuanDinliyorSesKes = false;
  public boolean Listedevarmi = true;

  public static String localLang;

  public static Button button_dinle_ara;
  public static Button button_kesfet;


  ArrayList<String> voiceResults;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.tfe_od_activity_camera);
//    Toolbar toolbar = findViewById(R.id.toolbar);
//    setSupportActionBar(toolbar);
//    getSupportActionBar().setDisplayShowTitleEnabled(false);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    threadsTextView = findViewById(R.id.threads);
    plusImageView = findViewById(R.id.plus);
    minusImageView = findViewById(R.id.minus);
    apiSwitchCompat = findViewById(R.id.api_info_switch);
    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);

    frameValueTextView = findViewById(R.id.frame_info);
    cropValueTextView = findViewById(R.id.crop_info);
    inferenceTimeTextView = findViewById(R.id.inference_info);

    button_dinle_ara = findViewById(R.id.button_dinle_ara);
    button_kesfet = findViewById(R.id.button_kesfet);

    sagda = "sağda";
    solda = "solda";
    ortada = "ortada";
    gorunuyor="gorunuyor";
    bulunamadi="bulunamadı";
    uzakta="uzakta";
    yakinda="yakında";

    localLang = Locale.getDefault().getDisplayLanguage();


    if(mTTs == null){
      String engine = "com.google.android.tts";
      mTTs = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
          if (status == TextToSpeech.SUCCESS) {
            //int result = mTTs.setLanguage(new Locale("tr", "TR"));
            //CameraActivity.mTTs.setLanguage(Locale.forLanguageTag("English"));
            int result;


            mTTs.setOnUtteranceProgressListener(new UtteranceProgressListener() {
              @Override
              public void onStart(String utteranceId) {

              }

              @Override
              public void onDone(String utteranceId) {

              }

              @Override
              public void onError(String utteranceId) {

              }
            });

            if(!CameraActivity.localLang.contains("Türkçe")){
              result = mTTs.setLanguage(new Locale("en", ""));
            }
            else{
              result = mTTs.setLanguage(new Locale("tr", "TR"));
            }



            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {

              Log.e("TTS", "Dil Desteklenmiyor");
            }


            String utterence = this.hashCode() + "";
            mTTs.speak("uygulamamıza hoşgeldiniz nesneleri keşfetmek için sağ alttaki butona basınız nesne aramak için sol attaki butona basınız", TextToSpeech.QUEUE_ADD, null, utterence);
          //nesneleri keşfetmek için sağ alttaki butona basınız nesne aramak için sol attaki butona basınız

          }
        }

      },engine);
    }




    button_kesfet.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {

        BelirliArama = false;
        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
          if (kesfetBasla == false){
            kesfetBasla = true;
          }else{
            kesfetBasla = false;
          }
          System.out.println("kesfet pressed");
        }

        return true;
      }

    });

    button_dinle_ara.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {

        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){

          System.out.println("DİNLE TUŞUNA BASILDI1");
          kesfetBasla = false;

          DinleButon();
          recognizer.startListening(intent);


        }
        return false;
      }
    });
  }



  public void DinleButon() {

    mTTs.stop();

    intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
            this.getPackageName());


    recognizer = SpeechRecognizer
            .createSpeechRecognizer(this);

    System.out.println("Dinle button içerisindeyiz ");


    RecognitionListener listener = new RecognitionListener() {
      @Override
      public void onReadyForSpeech(Bundle params) {

        SuanDinliyorSesKes = true;
        kesfetBasla = false;
        BelirliArama = false;
        System.out.println("onReadyForSpeech ");

      }

      @Override
      public void onBeginningOfSpeech() {


      }

      @Override
      public void onRmsChanged(float rmsdB) {

      }

      @Override
      public void onBufferReceived(byte[] buffer) {

      }

      @Override
      public void onEndOfSpeech() {

      }

      @Override
      public void onError(int error) {

        System.out.println("ERROR İS " + error);

        //mTTs.speak(getText(R.string.Anlasilmadi),TextToSpeech.QUEUE_FLUSH,null,null);
        mTTs.speak("Error ",TextToSpeech.QUEUE_FLUSH,null,null);
        SuanDinliyorSesKes = false;
      }


      @Override
      public void onResults(Bundle results) {


        aranan = "results bos";
        voiceResults = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if(voiceResults.get(0) != null){
          aranan = voiceResults.get(0);
        }

        HashMap<String, String> mapTrToEng = new HashMap<String, String>();
        mapTrToEng.put("ayakkabı", "shoe");
        mapTrToEng.put("gözlük", "eye glasses");
        mapTrToEng.put("sırt çantası", "handbag");
        mapTrToEng.put("kravat", "tie");
        mapTrToEng.put("klavye", "keyboard");
        mapTrToEng.put("fare", "mouse");
        mapTrToEng.put("monitör", "tv");
        mapTrToEng.put("insan", "person");
        mapTrToEng.put("bisiklet", "bicycle");
        mapTrToEng.put("araba", "car");
        mapTrToEng.put( "trafik lambası", "traffic light");
        mapTrToEng.put("yangın musluğu", "fire hydrant");
        mapTrToEng.put("trafik lambası", "street sign");
        mapTrToEng.put("şapka", "hat");
        mapTrToEng.put("sırt çantası", "backpack");
        mapTrToEng.put("şemsiye", "umbrella");
        mapTrToEng.put("bavul", "suitcase");
        mapTrToEng.put("tv", "tv");
        mapTrToEng.put("şişe","bottle");
        mapTrToEng.put("tabak", "plate");
        mapTrToEng.put("şarap kadehi", "wine glass");
        mapTrToEng.put("fincan", "cup");
        mapTrToEng.put("bıçak", "knife");
        mapTrToEng.put("çatal", "fork");
        mapTrToEng.put("kaşık", "spoon");
        mapTrToEng.put("tas", "bowl");
        mapTrToEng.put("muz", "banana");
        mapTrToEng.put("elma", "apple");
        mapTrToEng.put("sandviç", "sandwich");
        mapTrToEng.put("portakal", "orange" );
        mapTrToEng.put("sandalye", "chair");
        mapTrToEng.put("kanepe", "couch");
        mapTrToEng.put("saksı bitkisi", "potted plant");
        mapTrToEng.put("yatak", "bed");
        mapTrToEng.put("pencere", "window");
        mapTrToEng.put("masa", "desk");
        mapTrToEng.put("kapı", "door");
        mapTrToEng.put("leptop", "laptop");
        mapTrToEng.put("telefon", "cell phone");
        mapTrToEng.put("kitap", "book");
        mapTrToEng.put("vazo", "vase");
        mapTrToEng.put("makas", "scissors");
        mapTrToEng.put("diş fırçası", "toothbrush");
        mapTrToEng.put("saç fırçası", "hair brush");


        for(int i = 0; i < taninan_neseler.length;i++){  //**********************************************************************

          System.out.println("Listedeki Eleman"+taninan_neseler[i]);
          if(taninan_neseler[i].equals(aranan)){

            for (String xx : mapTrToEng.keySet()) { //////////////////////////////// TR to ENG
              if(xx.equals(aranan)){
                aranan = mapTrToEng.get(xx);
                break;
              }
            }

            BelirliArama = true;
            Listedevarmi = true;
            break;
          }else{
            BelirliArama = false;
            Listedevarmi = false;
            SuanDinliyorSesKes = false;
          }

        }

        if(!Listedevarmi){
          mTTs.speak("anlaşılmadı, tekrar söyler misiniz?",TextToSpeech.QUEUE_FLUSH,null,null);
          BelirliArama = false;
          SuanDinliyorSesKes = false;
        }


             /*   if ( voiceResults.get(0) != null) {
                    aranan = voiceResults.get(0);
                    BelirliArama = true;
                    System.out.println("ARANAN KELİME " + aranan);

                }*/



        if (voiceResults == null) {
          System.out.println("No voice results");
        }

      }


      @Override
      public void onPartialResults(Bundle partialResults) {


      }

      @Override
      public void onEvent(int eventType, Bundle params) {

      }
    };

    recognizer.setRecognitionListener(listener);




  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();
  }

  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  CameraActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setUseNNAPI(isChecked);
    if (isChecked) apiSwitchCompat.setText("NNAPI");
    else apiSwitchCompat.setText("TFLITE");
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.plus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      numThreads++;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    } else if (v.getId() == R.id.minus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      numThreads--;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    }
  }

  protected void showFrameInfo(String frameInfo) {
    frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
    cropValueTextView.setText(cropInfo);
  }

  protected void showInference(String inferenceTime) {
    inferenceTimeTextView.setText(inferenceTime);
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void setNumThreads(int numThreads);

  protected abstract void setUseNNAPI(boolean isChecked);
}
