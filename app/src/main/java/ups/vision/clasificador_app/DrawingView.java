package ups.vision.clasificador_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {
    private Paint paint;
    private Path path;

    // Constructor que recibe solo el contexto
    public DrawingView(Context context) {
        super(context);
        init();
    }

    // Constructor que recibe contexto y atributos del XML
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // Método de inicialización común para ambos constructores
    private void init() {
        // Establecer el fondo a negro
        setBackgroundColor(android.graphics.Color.BLACK);

        // Configurar el Paint para las líneas blancas
        paint = new Paint();
        paint.setColor(android.graphics.Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            path.moveTo(x, y);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            path.lineTo(x, y);
        }
        invalidate();
        return true;
    }

    // Método para obtener el Bitmap de lo dibujado
    public Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    // Método para limpiar el área de dibujo
    public void limpiar() {
        path.reset();  // Resetea el Path, eliminando todo lo dibujado
        invalidate();  // Redibuja la vista vacía
    }
}