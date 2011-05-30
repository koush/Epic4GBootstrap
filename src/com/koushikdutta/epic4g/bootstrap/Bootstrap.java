package com.koushikdutta.epic4g.bootstrap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.koushikdutta.verizontab.bootstrap.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Bootstrap extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        unzipAssets();
        
        Button flashBoot = (Button)findViewById(R.id.flash_boot);
        flashBoot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String filesDir = getFilesDir().getAbsolutePath();
                String flash_image = filesDir + "/flash_image";
                String bootimg = filesDir + "/boot.img";

                StringBuilder command = new StringBuilder();
                command.append(String.format("%s boot %s ; sync", flash_image, bootimg));
                
                AlertDialog.Builder builder = new Builder(Bootstrap.this);
                builder.setPositiveButton(android.R.string.ok, null);
                try {
                    Helper.runSuCommand(Bootstrap.this, command.toString());
                    builder.setMessage("Success!");
                }
                catch (Exception e) {
                    builder.setTitle("Failure");
                    builder.setMessage(e.getMessage());
                    e.printStackTrace();
                }
                builder.create().show();
            }
        });
        
        Button flashRecovery = (Button)findViewById(R.id.flash_recovery);
        flashRecovery.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String filesDir = getFilesDir().getAbsolutePath();
                String flash_image = filesDir + "/flash_image";
                String recoveryimg = filesDir + "/recovery.img";

                StringBuilder command = new StringBuilder();
                command.append(String.format("%s recoveryonly %s ; sync", flash_image, recoveryimg));
                
                AlertDialog.Builder builder = new Builder(Bootstrap.this);
                builder.setPositiveButton(android.R.string.ok, null);
                try {
                    Helper.runSuCommand(Bootstrap.this, command.toString());
                    builder.setMessage("Success!");
                }
                catch (Exception e) {
                    builder.setTitle("Failure");
                    builder.setMessage(e.getMessage());
                    e.printStackTrace();
                }
                builder.create().show();
            }
        });
        
        Button reboot = (Button)findViewById(R.id.reboot);
        reboot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder command = new StringBuilder();
                command.append("reboot recovery; ");
                try {
                    Helper.runSuCommand(Bootstrap.this, command.toString());
                }
                catch (Exception e) {
                    AlertDialog.Builder builder = new Builder(Bootstrap.this);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setTitle("Failure");
                    builder.setMessage(e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    final static String LOGTAG = "Epic4GBootstrap";
    final static String ZIP_FILTER = "assets";
    
    void unzipAssets() {
        String apkPath = getPackageCodePath();
        String mAppRoot = getFilesDir().toString();
        try {
            File zipFile = new File(apkPath);
            long zipLastModified = zipFile.lastModified();
            ZipFile zip = new ZipFile(apkPath);
            Vector<ZipEntry> files = getAssets(zip);
            int zipFilterLength = ZIP_FILTER.length();
            
            Enumeration<?> entries = files.elements();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String path = entry.getName().substring(zipFilterLength);
                File outputFile = new File(mAppRoot, path);
                outputFile.getParentFile().mkdirs();

                if (outputFile.exists() && entry.getSize() == outputFile.length() && zipLastModified < outputFile.lastModified())
                    continue;
                FileOutputStream fos = new FileOutputStream(outputFile);
                copyStreams(zip.getInputStream(entry), fos);
                Runtime.getRuntime().exec("chmod 755 " + outputFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "Error: " + e.getMessage());
        }
    }

    static final int BUFSIZE = 5192;

    void copyStreams(InputStream is, FileOutputStream fos) {
        BufferedOutputStream os = null;
        try {
            byte data[] = new byte[BUFSIZE];
            int count;
            os = new BufferedOutputStream(fos, BUFSIZE);
            while ((count = is.read(data, 0, BUFSIZE)) != -1) {
                os.write(data, 0, count);
            }
            os.flush();
        } catch (IOException e) {
            Log.e(LOGTAG, "Exception while copying: " + e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e2) {
                Log.e(LOGTAG, "Exception while closing the stream: " + e2);
            }
        }
    }

    public Vector<ZipEntry> getAssets(ZipFile zip) {
        Vector<ZipEntry> list = new Vector<ZipEntry>();
        Enumeration<?> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().startsWith(ZIP_FILTER)) {
                list.add(entry);
            }
        }
        return list;
    }
}