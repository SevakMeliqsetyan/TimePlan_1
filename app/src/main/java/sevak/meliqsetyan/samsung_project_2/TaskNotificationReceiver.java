package sevak.meliqsetyan.samsung_project_2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

public class TaskNotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "task_notifications";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TASK_TITLE);
        if (title == null || title.isEmpty()) return;

        // "Wake" the processor to ensure the notification is shown even if the device is sleeping
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimePlan:NotificationWake");
        wl.acquire(5000);

        showNotification(context, title);

        if (wl.isHeld()) wl.release();
    }

    private void showNotification(Context context, String title) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, 
                    "Task Reminders", 
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for your personal tasks");
            channel.enableVibration(true);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent activityIntent = new Intent(context, LoginActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pi = PendingIntent.getActivity(
                context, 
                (int) System.currentTimeMillis(), 
                activityIntent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_home)
                .setContentTitle("Upcoming Task")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi);

        if (manager != null) manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}