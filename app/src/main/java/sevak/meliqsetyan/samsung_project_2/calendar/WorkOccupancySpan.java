package sevak.meliqsetyan.samsung_project_2.calendar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.LineBackgroundSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Отрисовывает индикатор занятости в виде горизонтальных сегментов под текстом даты.
 * Используется для визуального отображения количества забронированных слотов.
 */
public class WorkOccupancySpan implements LineBackgroundSpan {

    private final int busyCount;
    private final int totalSlots;
    @ColorInt
    private final int busyColor;
    @ColorInt
    private final int idleColor;

    /**
     * @param busyCount  Количество занятых слотов (будут закрашены ярким цветом).
     * @param totalSlots Общее количество доступных слотов.
     * @param busyColor  Цвет занятого слота.
     * @param idleColor  Цвет свободного слота (фоновый).
     */
    public WorkOccupancySpan(int busyCount, int totalSlots, @ColorInt int busyColor, @ColorInt int idleColor) {
        this.busyCount = Math.max(0, busyCount);
        this.totalSlots = Math.max(1, totalSlots);
        this.busyColor = busyColor;
        this.idleColor = idleColor;
    }

    @Override
    public void drawBackground(
            @NonNull Canvas canvas,
            @NonNull Paint paint,
            int left, int right, int top, int baseline, int bottom,
            @NonNull CharSequence text, int start, int end, int lnum
    ) {
        // Сохраняем текущие настройки кисти
        int savedColor = paint.getColor();
        Paint.Style savedStyle = paint.getStyle();
        float savedStrokeWidth = paint.getStrokeWidth();

        // Расчет размеров области рисования
        float width = (right - left);
        float padding = width * 0.14f; // Отступы по бокам
        float gap = width * 0.02f;     // Промежуток между сегментами
        float barLeft = left + padding;
        float barRight = right - padding;
        float barWidth = Math.max(1f, barRight - barLeft);

        // Расчет размеров каждого сегмента
        float totalGaps = gap * (totalSlots - 1);
        float segmentWidth = (barWidth - totalGaps) / totalSlots;
        float segmentHeight = Math.max(4f, (bottom - top) * 0.08f); // Высота полоски

        // Позиционирование по вертикали (чуть выше самого низа ячейки)
        float y = bottom - segmentHeight - (bottom - top) * 0.10f;
        float radius = segmentHeight / 2f; // Скругление углов

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0f);

        RectF rect = new RectF();

        // Рисуем каждый слот
        for (int i = 0; i < totalSlots; i++) {
            float x1 = barLeft + i * (segmentWidth + gap);
            float x2 = x1 + segmentWidth;

            rect.set(x1, y, x2, y + segmentHeight);

            // Если индекс текущего слота меньше количества занятых — красим в busyColor
            paint.setColor(i < busyCount ? busyColor : idleColor);

            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        // Восстанавливаем настройки кисти
        paint.setColor(savedColor);
        paint.setStyle(savedStyle);
        paint.setStrokeWidth(savedStrokeWidth);
    }
}