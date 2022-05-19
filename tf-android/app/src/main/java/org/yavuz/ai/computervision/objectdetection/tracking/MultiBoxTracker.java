
package org.yavuz.ai.computervision.objectdetection.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.yavuz.ai.computervision.objectdetection.CameraActivity;
import org.yavuz.ai.computervision.objectdetection.DetectorActivity;
import org.yavuz.ai.computervision.objectdetection.env.BorderedText;
import org.yavuz.ai.computervision.objectdetection.env.ImageUtils;
import org.yavuz.ai.computervision.objectdetection.env.Logger;
import org.yavuz.ai.computervision.objectdetection.tflite.Detector.Recognition;

public class MultiBoxTracker {
  private static final float TEXT_SIZE_DIP = 18;
  private static final float MIN_SIZE = 16.0f;
  private static final int[] COLORS = {
    Color.BLUE,
    Color.RED,
    Color.GREEN,
    Color.YELLOW,
    Color.CYAN,
    Color.MAGENTA,
    Color.WHITE,
    Color.parseColor("#55FF55"),
    Color.parseColor("#FFA500"),
    Color.parseColor("#FF8888"),
    Color.parseColor("#AAAAFF"),
    Color.parseColor("#FFFFAA"),
    Color.parseColor("#55AAAA"),
    Color.parseColor("#AA33AA"),
    Color.parseColor("#0D0068")
  };
  final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
  private final Logger logger = new Logger();
  private final Queue<Integer> availableColors = new LinkedList<Integer>();
  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
  private final Paint boxPaint = new Paint();
  private final float textSizePx;
  private final BorderedText borderedText;
  private Matrix frameToCanvasMatrix;
  private int frameWidth;
  private int frameHeight;
  private int sensorOrientation;


  long diff, StartTime;
  public String lastSay;


  public MultiBoxTracker(final Context context) {
    for (final int color : COLORS) {
      availableColors.add(color);
    }

    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(10.0f);
    boxPaint.setStrokeCap(Cap.ROUND);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
  }

  public synchronized void setFrameConfiguration(
      final int width, final int height, final int sensorOrientation) {
    frameWidth = width;
    frameHeight = height;
    this.sensorOrientation = sensorOrientation;
  }

  public synchronized void drawDebug(final Canvas canvas) {
    final Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(60.0f);

    final Paint boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setAlpha(200);
    boxPaint.setStyle(Style.STROKE);

    for (final Pair<Float, RectF> detection : screenRects) {
      final RectF rect = detection.second;
      canvas.drawRect(rect, boxPaint);
      canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
      borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
    }
  }

  public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
    logger.i("Processing %d results from %d", results.size(), timestamp);
    processResults(results);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

  public synchronized void draw(final Canvas canvas) {
    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
        Math.min(
            canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
            canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
    frameToCanvasMatrix =
        ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (int) (multiplier * (rotated ? frameHeight : frameWidth)),
            (int) (multiplier * (rotated ? frameWidth : frameHeight)),
            sensorOrientation,
            false);


    for (final TrackedRecognition recognition : trackedObjects) {
      final RectF trackedPos = new RectF(recognition.location);

      getFrameToCanvasMatrix().mapRect(trackedPos);
      boxPaint.setColor(recognition.color);

      float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
      canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

      final String labelString =
              !TextUtils.isEmpty(recognition.title)
                      ? String.format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
                      : String.format("%.2f", (100 * recognition.detectionConfidence));
      //            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
      // labelString);
      borderedText.drawText(
              canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "%", boxPaint);


      HashMap<String, String> mapEngToTr = new HashMap<String, String>();
      mapEngToTr.put("shoe", "ayakkabı");
      mapEngToTr.put("eye glasses", "gözlük");
      mapEngToTr.put("handbag", "el çantası");
      mapEngToTr.put("tie", "kravat");
      mapEngToTr.put("keyboard", "klavye");
      mapEngToTr.put("mouse", "fare");
      mapEngToTr.put("tv", "tv");
      mapEngToTr.put("person", "insan");
      mapEngToTr.put("bicycle", "bisiklet");
      mapEngToTr.put("car", "araba");
      mapEngToTr.put("traffic light", "trafik lambası");
      mapEngToTr.put("fire hydrant", "yangın musluğu");
      mapEngToTr.put("street sign", "trafik lambası");
      mapEngToTr.put("hat", "şapka");
      mapEngToTr.put("backpack", "sırt çantası");
      mapEngToTr.put("umbrella", "şemsiye");
      mapEngToTr.put("suitcase", "bavul");
      mapEngToTr.put("tv", "tv");
      mapEngToTr.put("bottle", "şişe");
      mapEngToTr.put("plate", "tabak");
      mapEngToTr.put("wine glass", "şarap kadehi");
      mapEngToTr.put("cup", "fincan");
      mapEngToTr.put("knife", "bıçak");
      mapEngToTr.put("tv", "tv");
      mapEngToTr.put("fork", "çatal");
      mapEngToTr.put("spoon", "kaşık");
      mapEngToTr.put("bowl", "tas");
      mapEngToTr.put("banana", "muz");
      mapEngToTr.put("apple", "elma");
      mapEngToTr.put("sandwich", "sandviç");
      mapEngToTr.put("orange", "portakal");
      mapEngToTr.put("chair", "sandalye");
      mapEngToTr.put("couch", "kanepe");
      mapEngToTr.put("potted plant", "saksı bitkisi");
      mapEngToTr.put("bed", "yatak");
      mapEngToTr.put("window", "pencere");
      mapEngToTr.put("desk", "masa");
      mapEngToTr.put("door", "kapı");
      mapEngToTr.put("laptop", "leptop");
      mapEngToTr.put("cell phone", "telefon");
      mapEngToTr.put("book", "kitap");
      mapEngToTr.put("clock", "saat");
      mapEngToTr.put("vase", "vazo");
      mapEngToTr.put("scissors", "makas");
      mapEngToTr.put("toothbrush", "diş fırçası");
      mapEngToTr.put("hair brush", "saç fırçası");


      String kesfetTemp = recognition.title;

   //   int centerofScreen = canvas.getWidth() / 2;
    //  float centerX = (float) trackedPos.centerX();


      int centerofScreen = canvas.getWidth() / 2;
      float centerX = (float) trackedPos.centerX();


      float trackedW = (float) trackedPos.width();
      float trackedH = (float) trackedPos.height();
      float screenW = (float) canvas.getWidth();
      float screenH = (float) canvas.getHeight();


      String  distance = distancePredictor(screenW,screenH,trackedW,trackedH,recognition.title);
     // String distance = CameraActivity.uzakta;

      if (CameraActivity.kesfetBasla && CameraActivity.BelirliArama == false){
        if (recognition.detectionConfidence > 0.50f) {

          if (CameraActivity.mTTs != null) {

            diff = System.currentTimeMillis() - StartTime;

            if(!CameraActivity.mTTs.isSpeaking() && (lastSay != recognition.title || diff/1000 >=2)){
              if (!CameraActivity.mTTs.isSpeaking()) {

                StartTime = System.currentTimeMillis();


                  for (String xx : mapEngToTr.keySet()) { //////////////////////////////// ENG to TR
                    if(xx.equals(kesfetTemp)){
                      kesfetTemp = mapEngToTr.get(xx);
                      break;
                    }
                  }



                System.out.println("centerX " + centerX);
                System.out.println(recognition.title);
                if (centerX < centerofScreen - 45) {
                  String utterence = this.hashCode() + "";
                  CameraActivity.mTTs.speak(kesfetTemp +distance+  CameraActivity.solda, TextToSpeech.QUEUE_ADD, null, utterence);
                } else if (centerX > centerofScreen + 45) {
                  String utterence = this.hashCode() + "";
                  CameraActivity.mTTs.speak(kesfetTemp + distance+ CameraActivity.sagda, TextToSpeech.QUEUE_ADD, null, utterence);
                } else {
                  String utterence = this.hashCode() + "";
                  CameraActivity.mTTs.speak(kesfetTemp + distance+ CameraActivity.ortada, TextToSpeech.QUEUE_ADD, null, utterence);
                }
              }

              lastSay = recognition.title;
              continue;
            }
          }
        }
      }

      if (CameraActivity.BelirliArama && CameraActivity.mTTs != null) {

        CameraActivity.kesfetBasla = false;

        String gorulen = recognition.title; //manuel

        distance = distancePredictor(screenW,screenH,trackedW,trackedH,recognition.title); // DISTANCE
       // distance = CameraActivity.uzakta;

        if (gorulen.equals(CameraActivity.aranan)) {

          for (String xx : mapEngToTr.keySet()) { //////////////////////////////// ENG to TR
            if(xx.equals(gorulen)){
              gorulen = mapEngToTr.get(xx);
              break;
            }
          }



          if (!CameraActivity.mTTs.isSpeaking()) {
            if (centerX < centerofScreen - 45) {
              String utterence = this.hashCode() + "";
              CameraActivity.mTTs.speak(gorulen + distance+ CameraActivity.solda, TextToSpeech.QUEUE_ADD, null, utterence);
            } else if (centerX > centerofScreen + 45) {
              String utterence = this.hashCode() + "";
              CameraActivity.mTTs.speak(gorulen +distance+  CameraActivity.sagda, TextToSpeech.QUEUE_ADD, null, utterence);
            } else {
              String utterence = this.hashCode() + "";
              CameraActivity.mTTs.speak(gorulen + distance+ CameraActivity.ortada, TextToSpeech.QUEUE_ADD, null, utterence);
            }
          }
        } else {
          String tempAranan = "";//manuel
          for (String xx : mapEngToTr.keySet()) { //////////////////////////////// ENG to TR
            if(xx.equals(CameraActivity.aranan )){
              tempAranan  = mapEngToTr.get(xx);
              break;
            }
          }

          if (!CameraActivity.mTTs.isSpeaking()) {
            CameraActivity.mTTs.speak(tempAranan + CameraActivity.bulunamadi, TextToSpeech.QUEUE_FLUSH, null);
          }
        }
      }

    }
  }


  public String distancePredictor(float screenW, float screenH, float objectW, float objectH, String objectName){

    float canvasArea = screenW * screenH;
    float objectArea = objectW * objectH;

    float objectPercentage = (objectArea / canvasArea) * 100;

    System.out.println("objectPercentage "+objectPercentage);

    if(objectName.equals("laptop")){
      if (objectPercentage >= 50)
      {
        return CameraActivity.yakinda;
      }
      else
      {
        return  CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("mouse")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("keyboard")){
      if (objectPercentage >= 30)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("tv")){
      if (objectPercentage >= 50)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("eye glasses")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("handbag")){
      if (objectPercentage >= 30)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("hat")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("backpack")){
      if (objectPercentage >= 50)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("cup")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("bowl")){
      if (objectPercentage >= 50)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("chair")){
      if (objectPercentage >= 60)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("couch")){
      if (objectPercentage >= 80)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("bed")){
      if (objectPercentage >= 80)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("desk")){
      if (objectPercentage >= 60)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("cell phone")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("book")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("clock")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else if(objectName.equals("toothbrush")){
      if (objectPercentage >= 20)
      {
        return  CameraActivity.yakinda;
      }
      else
      {
        return CameraActivity.uzakta;
      }
    }
    else return "error";
  }



  private void processResults(final List<Recognition> results) {
    final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

    for (final Recognition result : results) {
      if (result.getLocation() == null) {
        continue;
      }
      final RectF detectionFrameRect = new RectF(result.getLocation());

      final RectF detectionScreenRect = new RectF();
      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

//      logger.v(
//          "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

      screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        logger.w("Degenerate rectangle! " + detectionFrameRect);
        continue;
      }

      rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
    }

    trackedObjects.clear();
    if (rectsToTrack.isEmpty()) {
      logger.v("Nothing to track, aborting.");
      return;
    }

    for (final Pair<Float, Recognition> potential : rectsToTrack) {
      final TrackedRecognition trackedRecognition = new TrackedRecognition();
      trackedRecognition.detectionConfidence = potential.first;
      trackedRecognition.location = new RectF(potential.second.getLocation());
      trackedRecognition.title = potential.second.getTitle();
      trackedRecognition.color = COLORS[trackedObjects.size()];
      trackedObjects.add(trackedRecognition);

      if (trackedObjects.size() >= COLORS.length) {
        break;
      }
    }
  }

  private static class TrackedRecognition {
    RectF location;
    float detectionConfidence;
    int color;
    String title;
  }
}
