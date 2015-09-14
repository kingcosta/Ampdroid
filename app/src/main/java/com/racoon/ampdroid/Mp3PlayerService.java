/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Daniel Schruhl
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package com.racoon.ampdroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import com.racoon.ampache.Song;

import org.apache.http.util.ByteArrayBuffer;

public class Mp3PlayerService extends Service {

	private MediaPlayer mediaPlayer;
	private ArrayList<Song> playList;
	private int cursor;
	private Song currentSong;
	private NotificationManager notifManager;
	public static final int NOTIFICATION_ID = 1556;
	private boolean pause;
	private boolean allowRebind;
	private String session;
    private String oCuser;
    private String oCpassword;

	@Override
	public void onCreate() {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setOnCompletionListener(new SongComplitionListener());
		pause = false;
		session = "";

	}

	@SuppressWarnings("unchecked")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getStringExtra("ACTION");
            oCuser = intent.getStringExtra("oCuser");
            oCpassword = intent.getStringExtra("oCpassword");

			if (action != null && intent.getSerializableExtra("com.racoon.ampdroid.NowPlaying") != null) {
				playList = (ArrayList<Song>) intent.getSerializableExtra("com.racoon.ampdroid.NowPlaying");
				if (action.equals("play") && !mediaPlayer.isPlaying()) {
					cursor = intent.getIntExtra("CURSOR", 0);
					session = intent.getStringExtra("SESSION");
					play(cursor);
					Log.d("service", "cursor: " + String.valueOf(cursor));
				} else if (action.equals("pause")) {
					pause();
				} else if (action.equals("next")) {
					next();
				} else if (action.equals("previous")) {
					previous();
				}

			} else {
				// TODO error handling
			}
		}

		return 0;
	}

	@Override
	public void onDestroy() {
		mediaPlayer.release();
		// mediaPlayer.reset();
		super.onDestroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// All clients have unbound with unbindService()
		return allowRebind;
	}

	@Override
	public void onRebind(Intent intent) {
		// A client is binding to the service with bindService(),
		// after onUnbind() has already been called
	}

	public void pause() {
		Log.d("service", "status: " + pause);
		if (pause) {
			pause = false;
			mediaPlayer.start();
		} else {
			pause = true;
			mediaPlayer.pause();
		}
		setNotifiction();
	}

	public void stop() {
		mediaPlayer.reset();
		setNotifiction();
		currentSong = null;
		stopSelf();
	}

	public void next() {
		if (playList != null) {
			int size = playList.size();
			if ((cursor + 1) < size) {
				cursor++;
				play(cursor);
			} else {
				stop();
			}
		}
	}

	public void previous() {
		if ((cursor - 1) >= 0) {
			cursor--;
			play(cursor);
		} else {
			stop();
		}
	}

    public void DownloadFromUrl(String DownloadURL, String LocalFileName, String User, String Password) {
        try {
            URL url = new URL(DownloadURL);
            File file = new File(LocalFileName);

            long startTime = System.currentTimeMillis();
            //Log.d("bugs", "download begining");
            //Log.d("bugs", "download url:" + url);
            //Log.d("bugs", "download to file:" + fileName);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            String credentials = User+":"+Password;
            credentials = Base64.encodeToString(credentials.getBytes(), Base64.DEFAULT);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", "Basic " + credentials);
            con.connect();

            InputStream is = con.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            //Read bytes to the Buffer until there is nothing more to read(-1).
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }

            //Convert the Bytes read to a String.
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("bugs", "download ready in"
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " sec");

            con.disconnect();

        } catch (IOException e) {
            Log.d("bugs", "Error: " + e);
        }

    }

	private void play(int id) {
		mediaPlayer.reset();
		try {
			Log.d("bugs", "song url " + playList.get(id).getUrl());
			String pattern = "ssid=([a-z]|[0-9])*&";
			String dataUrl = playList.get(id).getUrl().replaceAll(pattern, "ssid=" + session + "&");
			Log.d("bugs", "session " + session);
			Log.d("bugs", "data url " + dataUrl);
            //mediaPlayer.setDataSource(dataUrl);

            File extStore = Environment.getExternalStorageDirectory();
            String tmpfilename = extStore.getPath() + "/tmp/Ampdroid.mp3";

            File tmpfile = new File(tmpfilename);
            tmpfile.delete();

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setContentText(getResources().getString(R.string.downloadinfo));
            int mNotificationId = 1;
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(mNotificationId, mBuilder.build());

            DownloadFromUrl(dataUrl, tmpfilename, oCuser, oCpassword);
            mediaPlayer.setDataSource(tmpfilename);

            mNotifyMgr.cancel(mNotificationId);

            mediaPlayer.prepare();
			currentSong = playList.get(id);
			mediaPlayer.start();
			setNotifiction();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class SongComplitionListener implements OnCompletionListener {

		@Override
		public void onCompletion(MediaPlayer mp) {
			next();
		}
	}

	public String getCurrentTitle() {
		String result = "";
		if (currentSong != null) {
			result = currentSong.getTitle();
		}
		return result;
	}

	public String getArtist() {
		String result = "";
		if (currentSong != null) {
			result = currentSong.getArtist();
		}
		return result;
	}

	public boolean isPlaying() {
		boolean result = false;
		if (mediaPlayer != null) {
			try {
				result = mediaPlayer.isPlaying();
			} catch (IllegalStateException e) {
				Log.d("error", e.getStackTrace().toString());
			}
		}
		return result;
	}

	/**
	 * @return the mediaPlayer
	 */
	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}

	/**
	 * @return the currentSong
	 */
	public Song getCurrentSong() {
		return currentSong;
	}

	/**
	 * @return the cursor
	 */
	public int getCursor() {
		return cursor;
	}

	/**
	 * @return the session
	 */
	public String getSession() {
		return session;
	}

	/**
	 * @param session the session to set
	 */
	public void setSession(String session) {
		this.session = session;
	}

	/**
	 * Binding
	 */
	private final IBinder binder = new Mp3Binder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class Mp3Binder extends Binder {
		Mp3PlayerService getService() {
			return Mp3PlayerService.this;
		}
	}

	private void setNotifiction() {
		RemoteViews notificationView = new RemoteViews(this.getPackageName(), R.layout.player_notification);
		notificationView.setTextViewText(R.id.notificationSongArtist, getArtist());
		notificationView.setTextViewText(R.id.notificationSongTitle, getCurrentTitle());
		/* 1. Setup Notification Builder */
		Notification.Builder builder = new Notification.Builder(this);

		/* 2. Configure Notification Alarm */
		builder.setSmallIcon(R.drawable.ic_stat_notify).setAutoCancel(true).setWhen(System.currentTimeMillis())
				.setTicker(getCurrentTitle());

		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent notifIntent = PendingIntent.getActivity(this, 0, intent, 0);

		builder.setContentIntent(notifIntent);
		builder.setContent(notificationView);

		/* 4. Create Notification and use Manager to launch it */
        Notification notification = builder.getNotification();
        String ns = Context.NOTIFICATION_SERVICE;
        notifManager = (NotificationManager) getSystemService(ns);
        notifManager.notify(NOTIFICATION_ID, notification);
        startForeground(NOTIFICATION_ID, notification);
	}

}
