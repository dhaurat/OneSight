package com.onesight.uqac.onesight.controller;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class BitmapHandlingHelper
{
    static void setCroppedBitmap(String photoURI, ImageView iv)
    {
        Bitmap bitmap = BitmapFactory.decodeFile(photoURI);
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator;
        File outputFile = new File(path,"profile.png");
        try
        {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 500, 500);
            iv.setImageBitmap(getCroppedBitmap(bitmap, bitmap.getWidth()));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    static Bitmap getCroppedBitmap(Bitmap bmp, int radius) {
        Bitmap sbmp;

        if (bmp.getWidth() != radius || bmp.getHeight() != radius) {
            float smallest = Math.min(bmp.getWidth(), bmp.getHeight());
            float factor = smallest / radius;
            sbmp = Bitmap.createScaledBitmap(bmp,
                    (int) (bmp.getWidth() / factor),
                    (int) (bmp.getHeight() / factor), false);
        } else {
            sbmp = bmp;
        }

        Bitmap output = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final String color = "#BAB399";
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, radius, radius);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor(color));
        canvas.drawCircle(radius / 2 + 0.7f, radius / 2 + 0.7f,
                radius / 2 + 0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sbmp, rect, rect, paint);

        return output;
    }

    static Uri setCroppedBitmapAndSave(Context context, Bitmap bitmap, ImageView iv)
    {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator;
        File outputFile = new File(path,"profile.png");
        String imageURL = null;
        try
        {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 500, 500);
            iv.setImageBitmap(bitmap);
            outputStream.close();

            imageURL = MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    outputFile.getAbsolutePath(), outputFile.getName(), outputFile.getName());

        }
        catch (IOException io)
        {
            io.printStackTrace();
        }
        return Uri.parse(imageURL);
    }

    static String getRealPathFromURI(Uri contentURI, Activity context) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = context.getContentResolver().query(contentURI,
                filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        int columnIndex = 0;
        if (cursor != null) {
            columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        }
        String picturePath = null;
        if (cursor != null) {
            picturePath = cursor.getString(columnIndex);
        }
        if (cursor != null) {
            cursor.close();
        }
        return picturePath;
    }

    static File createImageFile(Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

}
